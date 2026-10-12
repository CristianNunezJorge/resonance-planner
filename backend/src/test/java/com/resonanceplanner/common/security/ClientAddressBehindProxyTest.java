package com.resonanceplanner.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * The rate limits behind the proxies of Render, with a real Tomcat and the real "prod" profile. On Render the request
 * comes from Cloudflare, then from a proxy of Render, and X-Forwarded-For ends with: the address a client wrote itself
 * (if it did), the real client (added by Cloudflare), the Cloudflare node (added by Render). The 12/10/2026 logs showed
 * the application counting a different Cloudflare node on almost every request, so nobody was ever limited.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.rate-limit.auth-attempts=3")
@ActiveProfiles({"test", "prod"})
class ClientAddressBehindProxyTest {

    private static final String FORGED = "203.0.113.77";
    private static final String CLOUDFLARE_NODE = "172.70.216.24";

    @LocalServerPort
    private int port;

    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void countsTheRealClientEvenWhenADifferentCloudflareNodeAnswersEveryTime() throws Exception {
        String realClient = "198.51.100.50";
        // One node for each range of Cloudflare: IPv4 of three sizes and IPv6
        String[] nodes = {"172.70.216.24", "104.18.5.5", "162.159.1.1", "2606:4700::1111"};

        for (int attempt = 0; attempt < 3; attempt++) {
            assertThat(loginThrough(FORGED + ", " + realClient + ", " + nodes[attempt])).isEqualTo(401);
        }

        assertThat(loginThrough(FORGED + ", " + realClient + ", " + nodes[3])).isEqualTo(429);
    }

    @Test
    void ignoresWhatTheClientWritesInTheForwardedHeaderItself() throws Exception {
        String realClient = "198.51.100.51";

        for (int attempt = 0; attempt < 3; attempt++) {
            // The client changes the first address at every request, to look like a new client
            assertThat(loginThrough("203.0.113." + attempt + ", " + realClient + ", " + CLOUDFLARE_NODE)).isEqualTo(401);
        }

        assertThat(loginThrough("203.0.113.99, " + realClient + ", " + CLOUDFLARE_NODE)).isEqualTo(429);
    }

    @Test
    void doesNotLimitAnotherClientWhoUsesTheSameCloudflareNode() throws Exception {
        for (int attempt = 0; attempt < 4; attempt++) {
            loginThrough("198.51.100.52, " + CLOUDFLARE_NODE);
        }

        assertThat(loginThrough("198.51.100.53, " + CLOUDFLARE_NODE)).isEqualTo(401);
    }

    @Test
    void countsTheRealClientWhenTheRequestDoesNotPassThroughCloudflare() throws Exception {
        String realClient = "198.51.100.54";

        for (int attempt = 0; attempt < 3; attempt++) {
            assertThat(loginThrough(FORGED + ", " + realClient)).isEqualTo(401);
        }

        assertThat(loginThrough(FORGED + ", " + realClient)).isEqualTo(429);
    }

    // The test client connects from the local computer, which Tomcat trusts as a proxy (like the proxy of Render)
    private int loginThrough(String forwardedFor) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .header("X-Forwarded-For", forwardedFor)
                .POST(HttpRequest.BodyPublishers.ofString("{\"email\":\"nobody@example.com\",\"password\":\"x\"}"))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
    }
}
