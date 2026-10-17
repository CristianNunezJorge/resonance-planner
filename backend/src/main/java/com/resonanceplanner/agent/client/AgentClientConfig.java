package com.resonanceplanner.agent.client;

import com.resonanceplanner.common.config.AppProperties;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

/** The model client of the agent: OpenRouter with the free model of the configuration (D126). */
@Configuration
public class AgentClientConfig {

    // D61: the model can take a while to answer, but a call never lasts more than 60 seconds
    static final Duration TIMEOUT = Duration.ofSeconds(60);
    static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);

    @Bean
    ModelClient modelClient(AppProperties properties, JsonMapper jsonMapper) {
        return openRouterClient(properties.agent(), jsonMapper);
    }

    static OpenRouterClient openRouterClient(AppProperties.Agent settings, JsonMapper jsonMapper) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
        RestClient restClient = RestClient.builder().requestFactory(new JdkClientHttpRequestFactory(httpClient)).build();
        return new OpenRouterClient(restClient, jsonMapper, settings.apiKey(), settings.model(), TIMEOUT);
    }
}
