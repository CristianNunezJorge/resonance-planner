package com.resonanceplanner.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import com.resonanceplanner.common.config.AppProperties;
import com.resonanceplanner.testsupport.MutableClock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

// Pure unit test of the rate limits of D48: small limits and a clock that moves only when the test says so
class RateLimitFilterTest {

    private static final Instant START = Instant.parse("2026-10-12T10:00:00Z");
    private static final String LOGIN = "/api/auth/login";
    private static final String REGISTER = "/api/auth/register";
    private static final String GUEST = "/api/auth/guest";
    private static final String CLIENT = "198.51.100.10";

    private final MutableClock clock = new MutableClock(START);

    // 3 attempts every 15 minutes for login and registration, 5 requests every minute for the rest
    private final AppProperties.RateLimit limits =
            new AppProperties.RateLimit(3, Duration.ofMinutes(15), 5, Duration.ofMinutes(1), 2);

    private final ApiSecurityErrorHandler errorWriter = new ApiSecurityErrorHandler(JsonMapper.builder().build());

    private final RateLimitFilter filter = new RateLimitFilter(limits, clock, errorWriter);

    private record Outcome(MockHttpServletResponse response, boolean passed) {}

    @Test
    void letsTheAttemptsOfTheLimitPassAndRefusesTheNextOneInTheApiFormat() throws Exception {
        for (int attempt = 1; attempt <= 3; attempt++) {
            assertThat(send("POST", LOGIN, CLIENT).passed()).as("attempt %d", attempt).isTrue();
        }

        Outcome refused = send("POST", LOGIN, CLIENT);

        assertThat(refused.passed()).isFalse();
        assertThat(refused.response().getStatus()).isEqualTo(429);
        assertThat(refused.response().getContentType()).startsWith("application/json");
        String body = refused.response().getContentAsString();
        assertThat((Boolean) JsonPath.read(body, "$.success")).isFalse();
        assertThat((String) JsonPath.read(body, "$.error.code")).isEqualTo("RATE_LIMITED");
        assertThat((String) JsonPath.read(body, "$.error.message")).isEqualTo("Troppe richieste: riprova tra 15 minuti.");
        assertThat(refused.response().getHeader(HttpHeaders.RETRY_AFTER)).isEqualTo("900");
    }

    @Test
    void tellsWhenToTryAgainAndShrinksTheWaitAsTimePasses() throws Exception {
        exhaust("POST", LOGIN, CLIENT, 3);

        clock.advance(Duration.ofSeconds(330));
        Outcome afterFiveAndAHalfMinutes = send("POST", LOGIN, CLIENT);
        assertThat(afterFiveAndAHalfMinutes.response().getHeader(HttpHeaders.RETRY_AFTER)).isEqualTo("570");
        assertThat(message(afterFiveAndAHalfMinutes)).isEqualTo("Troppe richieste: riprova tra 10 minuti.");

        clock.advance(Duration.ofSeconds(550));
        Outcome twentySecondsLeft = send("POST", LOGIN, CLIENT);
        assertThat(twentySecondsLeft.response().getHeader(HttpHeaders.RETRY_AFTER)).isEqualTo("20");
        assertThat(message(twentySecondsLeft)).isEqualTo("Troppe richieste: riprova tra 20 secondi.");
    }

    @Test
    void usesTheSingularForOneSecondAndOneMinute() throws Exception {
        exhaust("POST", LOGIN, CLIENT, 3);

        clock.advance(Duration.ofSeconds(899));
        assertThat(message(send("POST", LOGIN, CLIENT))).isEqualTo("Troppe richieste: riprova tra 1 secondo.");

        // A fresh window of one minute for the general limit: 60 seconds left is exactly one minute
        exhaust("GET", "/api/game/rules", "198.51.100.11", 5);
        assertThat(message(send("GET", "/api/game/rules", "198.51.100.11"))).isEqualTo("Troppe richieste: riprova tra 1 minuto.");
    }

    @Test
    void acceptsTheClientAgainWhenTheWindowEnds() throws Exception {
        exhaust("POST", LOGIN, CLIENT, 3);
        assertThat(send("POST", LOGIN, CLIENT).passed()).isFalse();

        clock.advance(Duration.ofMinutes(15));

        assertThat(send("POST", LOGIN, CLIENT).passed()).isTrue();
    }

    @Test
    void keepsALoginCounterAndARegistrationCounterForEachClient() throws Exception {
        exhaust("POST", LOGIN, CLIENT, 3);

        assertThat(send("POST", LOGIN, CLIENT).passed()).isFalse();
        assertThat(send("POST", REGISTER, CLIENT).passed()).isTrue();
        exhaust("POST", REGISTER, CLIENT, 2);
        assertThat(send("POST", REGISTER, CLIENT).passed()).isFalse();
    }

    @Test
    void givesTheGuestsACounterOfTheirOwnWithTheWindowOfTheAuthentication() throws Exception {
        // 2 guests every 15 minutes (D134), without touching login and registration
        exhaust("POST", GUEST, CLIENT, 2);

        Outcome refused = send("POST", GUEST, CLIENT);
        assertThat(refused.passed()).isFalse();
        assertThat(refused.response().getHeader(HttpHeaders.RETRY_AFTER)).isEqualTo("900");
        assertThat(send("POST", LOGIN, CLIENT).passed()).isTrue();
        assertThat(send("POST", REGISTER, CLIENT).passed()).isTrue();

        clock.advance(Duration.ofMinutes(15));
        assertThat(send("POST", GUEST, CLIENT).passed()).isTrue();
    }

    @Test
    void keepsSeparateCountersForDifferentClients() throws Exception {
        exhaust("POST", LOGIN, CLIENT, 3);

        assertThat(send("POST", LOGIN, CLIENT).passed()).isFalse();
        assertThat(send("POST", LOGIN, "198.51.100.99").passed()).isTrue();
    }

    @Test
    void appliesTheGeneralLimitToEveryOtherRequest() throws Exception {
        for (int request = 1; request <= 5; request++) {
            assertThat(send("GET", "/api/game/rules", CLIENT).passed()).as("request %d", request).isTrue();
        }

        Outcome refused = send("GET", "/api/me", CLIENT);

        assertThat(refused.passed()).isFalse();
        assertThat(refused.response().getStatus()).isEqualTo(429);
        assertThat(refused.response().getHeader(HttpHeaders.RETRY_AFTER)).isEqualTo("60");

        clock.advance(Duration.ofMinutes(1));
        assertThat(send("GET", "/api/game/rules", CLIENT).passed()).isTrue();
    }

    @Test
    void doesNotMixTheGeneralLimitWithTheLoginAndRegistrationLimits() throws Exception {
        exhaust("POST", LOGIN, CLIENT, 3);
        exhaust("POST", REGISTER, CLIENT, 3);

        for (int request = 1; request <= 5; request++) {
            assertThat(send("GET", "/api/game/rules", CLIENT).passed()).isTrue();
        }
    }

    @Test
    void treatsOtherMethodsOnTheLoginPathAsOrdinaryRequests() throws Exception {
        exhaust("GET", LOGIN, CLIENT, 5);

        assertThat(send("GET", LOGIN, CLIENT).passed()).isFalse();
        assertThat(send("POST", LOGIN, CLIENT).passed()).isTrue();
    }

    @Test
    void givesTheHealthCheckOfTheHostingServiceAGenerousCounterOfItsOwn() throws Exception {
        // 600 a minute: far more than the hosting service needs, but not unlimited, because the check uses the database
        exhaust("GET", "/actuator/health", CLIENT, 600);

        assertThat(send("GET", "/actuator/health", CLIENT).passed()).isFalse();
        assertThat(send("GET", "/api/game/rules", CLIENT).passed()).isTrue();
    }

    @Test
    void doesNotMixTheLoginCounterWithTheOtherMethodsOfTheLoginPath() throws Exception {
        exhaust("POST", LOGIN, CLIENT, 3);

        assertThat(send("HEAD", LOGIN, CLIENT).passed()).isTrue();
        assertThat(send("OPTIONS", LOGIN, CLIENT).passed()).isTrue();
    }

    @Test
    void countsAPercentEncodedPathInTheSameCounterAsTheNormalOne() throws Exception {
        exhaust("POST", LOGIN, CLIENT, 3);

        // %6C is an "l": the application routes this path to the login, so it must not escape the login counter
        assertThat(send("POST", "/api/auth/%6Cogin", CLIENT).passed()).isFalse();
    }

    @Test
    void keepsTheCountersOfTheWindowsThatAreStillOpenWhenItForgetsTheOldOnes() throws Exception {
        exhaust("POST", LOGIN, CLIENT, 3);

        // The forgetting is due after one minute: the login window lasts fifteen
        clock.advance(Duration.ofMinutes(2));
        Outcome refused = send("POST", LOGIN, CLIENT);

        assertThat(refused.passed()).isFalse();
        assertThat(refused.response().getHeader(HttpHeaders.RETRY_AFTER)).isEqualTo("780");
    }

    @Test
    void letsExactlyTheLimitPassWhenManyRequestsArriveTogether() throws Exception {
        int threads = 8;
        int requestsPerThread = 50;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger passed = new AtomicInteger();
        List<Future<?>> tasks = new ArrayList<>();
        for (int thread = 0; thread < threads; thread++) {
            tasks.add(pool.submit(() -> {
                start.await();
                for (int i = 0; i < requestsPerThread; i++) {
                    if (send("POST", LOGIN, CLIENT).passed()) {
                        passed.incrementAndGet();
                    }
                }
                return null;
            }));
        }
        start.countDown();
        for (Future<?> task : tasks) {
            task.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertThat(passed.get()).isEqualTo(3);
    }

    @Test
    void sendsEverythingThatIsNotAnAddressToOneSharedCounterInsteadOfCountingEachTextOnItsOwn() {
        String unknown = "unknown";
        assertThat(RateLimitFilter.clientKey(null)).isEqualTo(unknown);
        assertThat(RateLimitFilter.clientKey("")).isEqualTo(unknown);
        assertThat(RateLimitFilter.clientKey("   ")).isEqualTo(unknown);
        // A port attached: every connection would otherwise be a new client
        assertThat(RateLimitFilter.clientKey("203.0.113.7:51234")).isEqualTo(unknown);
        assertThat(RateLimitFilter.clientKey("[2001:db8::1]:443")).isEqualTo(unknown);
        assertThat(RateLimitFilter.clientKey("fe80::1%eth0")).isEqualTo(unknown);
        // Names and strange texts, including the one that starts with a dot and could trigger a name lookup
        assertThat(RateLimitFilter.clientKey("example.com")).isEqualTo(unknown);
        assertThat(RateLimitFilter.clientKey(".1:2")).isEqualTo(unknown);
        assertThat(RateLimitFilter.clientKey("dead.beef")).isEqualTo(unknown);
        assertThat(RateLimitFilter.clientKey("1:2:3:4:5:6:7:8:9")).isEqualTo(unknown);
        assertThat(RateLimitFilter.clientKey("a".repeat(10_000))).isEqualTo(unknown);
        assertThat(RateLimitFilter.clientKey("1.2.3.4.5")).isEqualTo(unknown);
    }

    @Test
    void readsPlainAndSpecialAddresses() {
        assertThat(RateLimitFilter.clientKey("203.0.113.7")).isEqualTo("203.0.113.7");
        assertThat(RateLimitFilter.clientKey("::ffff:c000:207")).isEqualTo("192.0.2.7");
        assertThat(RateLimitFilter.clientKey("::1")).isEqualTo("0000000000000000/64");
        assertThat(RateLimitFilter.clientKey("::")).isEqualTo("0000000000000000/64");
        assertThat(RateLimitFilter.clientKey("2001:DB8:1:2::9")).isEqualTo(RateLimitFilter.clientKey("2001:db8:1:2:5::1"));
    }

    @Test
    void givesOneCounterToAllTheAddressesOfTheSameIpv6Network() throws Exception {
        exhaust("POST", LOGIN, "2001:db8:1:2:aaaa:bbbb:cccc:1", 3);

        // Same first 64 bits: the same network of the same user, which owns billions of addresses
        assertThat(send("POST", LOGIN, "2001:db8:1:2:1111:2222:3333:4444").passed()).isFalse();
        assertThat(send("POST", LOGIN, "2001:db8:1:3:aaaa:bbbb:cccc:1").passed()).isTrue();
    }

    @Test
    void treatsAnIpv4AddressWrittenInIpv6FormAsTheSameClient() throws Exception {
        exhaust("POST", LOGIN, "192.0.2.7", 3);

        assertThat(send("POST", LOGIN, "::ffff:192.0.2.7").passed()).isFalse();
    }

    @Test
    void forgetsTheClientsWhoseWindowIsOver() throws Exception {
        send("GET", "/api/game/rules", "198.51.100.1");
        send("GET", "/api/game/rules", "198.51.100.2");
        send("POST", LOGIN, "198.51.100.3");
        assertThat(filter.trackedCounters()).isEqualTo(3);

        clock.advance(Duration.ofMinutes(16));
        send("GET", "/api/game/rules", "198.51.100.4");

        assertThat(filter.trackedCounters()).isEqualTo(1);
    }

    @Test
    void whenTheMemoryIsFullNewClientsCannotTryToLogInButTheRestOfTheApiStaysOpen() throws Exception {
        RateLimitFilter small = new RateLimitFilter(limits, clock, errorWriter, 2);
        assertThat(send(small, "POST", LOGIN, "198.51.100.1").passed()).isTrue();
        assertThat(send(small, "POST", LOGIN, "198.51.100.2").passed()).isTrue();

        // A new client: no counter can be created for the login, but the public catalog and the health check work
        Outcome stranger = send(small, "POST", LOGIN, "198.51.100.3");
        assertThat(stranger.passed()).isFalse();
        assertThat(stranger.response().getStatus()).isEqualTo(429);
        assertThat(send(small, "POST", REGISTER, "198.51.100.3").passed()).isFalse();
        assertThat(send(small, "GET", "/api/game/rules", "198.51.100.3").passed()).isTrue();
        assertThat(send(small, "GET", "/actuator/health", "198.51.100.3").passed()).isTrue();
        // A known client keeps being counted
        assertThat(send(small, "POST", LOGIN, "198.51.100.1").passed()).isTrue();

        clock.advance(Duration.ofMinutes(16));
        assertThat(send(small, "POST", LOGIN, "198.51.100.3").passed()).isTrue();
    }

    private void exhaust(String method, String path, String address, int times) throws Exception {
        for (int i = 0; i < times; i++) {
            assertThat(send(method, path, address).passed()).as("%s %s #%d", method, path, i + 1).isTrue();
        }
    }

    private Outcome send(String method, String path, String address) throws Exception {
        return send(filter, method, path, address);
    }

    private static Outcome send(RateLimitFilter target, String method, String path, String address) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRemoteAddr(address);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        target.doFilter(request, response, chain);
        return new Outcome(response, chain.getRequest() != null);
    }

    private static String message(Outcome outcome) throws Exception {
        return JsonPath.read(outcome.response().getContentAsString(), "$.error.message");
    }
}
