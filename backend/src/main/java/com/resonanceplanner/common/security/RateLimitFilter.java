package com.resonanceplanner.common.security;

import com.resonanceplanner.common.config.AppProperties;
import com.resonanceplanner.common.error.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UrlPathHelper;

/**
 * Limits the requests of each client address, with counters in memory (D48, D101): login and registration have a
 * counter each, the health check has its own generous one, every other request shares a general one. The window is
 * fixed: the first request opens it, and the counter starts again when it ends.
 * <p>
 * It runs before the controllers, so it writes the 429 itself in the ApiResponse format. Like the JWT filter,
 * SecurityConfig creates it: it is not a bean, otherwise Spring Boot would register it twice.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    // Counters in memory at the same time (about 180 bytes each). The keys are chosen by whoever sends the requests.
    private static final int DEFAULT_MAX_TRACKED_COUNTERS = 200_000;
    private static final Duration PURGE_INTERVAL = Duration.ofMinutes(1);
    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String REGISTER_PATH = "/api/auth/register";
    private static final String GUEST_PATH = "/api/auth/guest";
    // The hosting service calls it all the time: a 429 here could make it restart the application
    private static final String HEALTH_PATH = "/actuator/health";
    private static final Rule HEALTH_RULE = new Rule(600, Duration.ofMinutes(1));

    private static final String UNKNOWN_CLIENT = "unknown";
    private static final Pattern IPV4 = Pattern.compile("[0-9]{1,3}(\\.[0-9]{1,3}){3}");
    // Hex digits, colons and dots only, starting with a hex digit or a colon: the JDK reads it as a literal address
    private static final Pattern IPV6_CHARACTERS = Pattern.compile("[0-9a-fA-F:][0-9a-fA-F:.]{1,44}");
    private static final int LOGGED_HEADER_LENGTH = 200;

    // Auth buckets are refused when the memory is full; the others are let through without a counter
    private enum Bucket {
        LOGIN(true), REGISTER(true), GUEST(true), GENERAL(false), HEALTH(false);

        private final boolean protectsAccounts;

        Bucket(boolean protectsAccounts) {
            this.protectsAccounts = protectsAccounts;
        }
    }

    private record Rule(int requests, Duration window) {}

    private record Key(Bucket bucket, String client) {}

    private record Window(Instant end, int count) {}

    private final Map<Bucket, Rule> rules;
    private final Clock clock;
    private final ApiSecurityErrorHandler errorWriter;
    private final int maxTrackedCounters;
    private final ConcurrentHashMap<Key, Window> windows = new ConcurrentHashMap<>();
    private final AtomicReference<Instant> nextPurge;

    public RateLimitFilter(AppProperties.RateLimit limits, Clock clock, ApiSecurityErrorHandler errorWriter) {
        this(limits, clock, errorWriter, DEFAULT_MAX_TRACKED_COUNTERS);
    }

    RateLimitFilter(AppProperties.RateLimit limits, Clock clock, ApiSecurityErrorHandler errorWriter,
            int maxTrackedCounters) {
        Rule auth = new Rule(limits.authAttempts(), limits.authWindow());
        // A guest is an account too (D134): its counter has the window of the authentication
        Rule guest = new Rule(limits.guestCreations(), limits.authWindow());
        this.rules = new EnumMap<>(Map.of(Bucket.LOGIN, auth, Bucket.REGISTER, auth, Bucket.GUEST, guest,
                Bucket.GENERAL, new Rule(limits.generalRequests(), limits.generalWindow()), Bucket.HEALTH, HEALTH_RULE));
        this.clock = clock;
        this.errorWriter = errorWriter;
        this.maxTrackedCounters = maxTrackedCounters;
        this.nextPurge = new AtomicReference<>(clock.instant().plus(PURGE_INTERVAL));
        log.info("Rate limits: login and registration {} per {} each, guests {} per {}, other requests {} per {}",
                auth.requests(), auth.window(), guest.requests(), guest.window(), limits.generalRequests(),
                limits.generalWindow());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Bucket bucket = bucketOf(request);
        Instant now = clock.instant();
        purgeIfDue(now);

        String remoteAddress = request.getRemoteAddr();
        Key key = new Key(bucket, clientKey(remoteAddress));
        if (!windows.containsKey(key) && windows.size() >= maxTrackedCounters) {
            if (bucket.protectsAccounts) {
                // Memory protection: a new client cannot try to log in until the old counters are forgotten
                refuse(response, PURGE_INTERVAL.toSeconds());
            } else {
                chain.doFilter(request, response);
            }
            return;
        }
        Rule rule = rules.get(bucket);
        Window window = windows.compute(key, (k, current) -> current == null || !now.isBefore(current.end())
                ? new Window(now.plus(rule.window()), 1)
                : new Window(current.end(), current.count() + 1));

        if (window.count() <= rule.requests()) {
            chain.doFilter(request, response);
            return;
        }
        if (window.count() == rule.requests() + 1) {
            // Once per window, not for every refused request, so an attack cannot flood the logs. The three
            // headers show what the proxies of the hosting service send, to check that the key is the real client.
            log.warn("Rate limit reached: bucket={}, client={}, remoteAddr={}, x-forwarded-for={}, cf-connecting-ip={}",
                    bucket, key.client(), loggable(remoteAddress), loggable(request.getHeader("X-Forwarded-For")),
                    loggable(request.getHeader("CF-Connecting-IP")));
        }
        refuse(response, secondsUntil(window.end(), now));
    }

    private static Bucket bucketOf(HttpServletRequest request) {
        String path = UrlPathHelper.defaultInstance.getPathWithinApplication(request);
        if ("POST".equals(request.getMethod())) {
            if (LOGIN_PATH.equals(path)) {
                return Bucket.LOGIN;
            }
            if (REGISTER_PATH.equals(path)) {
                return Bucket.REGISTER;
            }
            if (GUEST_PATH.equals(path)) {
                return Bucket.GUEST;
            }
        }
        return HEALTH_PATH.equals(path) ? Bucket.HEALTH : Bucket.GENERAL;
    }

    // Forgets the counters whose window is over, at most once a minute
    private void purgeIfDue(Instant now) {
        Instant due = nextPurge.get();
        if (now.isBefore(due) || !nextPurge.compareAndSet(due, now.plus(PURGE_INTERVAL))) {
            return;
        }
        windows.entrySet().removeIf(entry -> !now.isBefore(entry.getValue().end()));
        if (windows.size() >= maxTrackedCounters) {
            log.warn("Rate limit memory is full ({} counters): login and registration of new clients are refused "
                    + "until old counters expire", windows.size());
        }
    }

    private void refuse(HttpServletResponse response, long retryAfterSeconds) throws IOException {
        long seconds = Math.max(1, retryAfterSeconds);
        response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(seconds));
        errorWriter.write(response, ErrorCode.RATE_LIMITED, "Troppe richieste: riprova tra " + waitText(seconds) + ".");
    }

    private static long secondsUntil(Instant end, Instant now) {
        long millis = Duration.between(now, end).toMillis();
        return (millis + 999) / 1000;
    }

    private static String waitText(long seconds) {
        if (seconds < 60) {
            return seconds + (seconds == 1 ? " secondo" : " secondi");
        }
        long minutes = (seconds + 59) / 60;
        return minutes + (minutes == 1 ? " minuto" : " minuti");
    }

    /**
     * The client the counters belong to. An IPv4 address counts as it is. An IPv6 user owns a whole /64 network
     * (billions of addresses), so the first 64 bits are used: otherwise changing address would reset the counter.
     * Anything that is not an address (a port attached, a name, a very long text) goes to one shared counter:
     * a wrong value limits too much, and it is seen at once, instead of not limiting at all.
     */
    static String clientKey(String address) {
        if (address == null) {
            return UNKNOWN_CLIENT;
        }
        if (IPV4.matcher(address).matches()) {
            return address;
        }
        if (address.indexOf(':') < 0 || !IPV6_CHARACTERS.matcher(address).matches()) {
            return UNKNOWN_CLIENT;
        }
        try {
            // A colon and nothing but address characters: the JDK reads it as a literal, no name lookup happens
            InetAddress parsed = InetAddress.getByName(address);
            if (parsed instanceof Inet6Address) {
                return HexFormat.of().formatHex(parsed.getAddress(), 0, 8) + "/64";
            }
            // An IPv4 address written in IPv6 form (::ffff:192.0.2.7)
            return parsed.getHostAddress();
        } catch (UnknownHostException e) {
            return UNKNOWN_CLIENT;
        }
    }

    // A value can come from outside: only harmless characters and a limited length go into the logs
    private static String loggable(String value) {
        if (value == null) {
            return "-";
        }
        String clean = value.replaceAll("[^0-9a-zA-Z:.,% ]", "?");
        return clean.length() > LOGGED_HEADER_LENGTH ? clean.substring(0, LOGGED_HEADER_LENGTH) + "..." : clean;
    }
}
