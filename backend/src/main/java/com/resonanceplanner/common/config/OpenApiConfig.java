package com.resonanceplanner.common.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger gets the "Authorize" button: paste the token of the login and the protected endpoints can be tried (D53).
 * Only the controllers that ask for {@link #BEARER_AUTH} show the lock.
 */
@Configuration(proxyBeanMethods = false)
@SecurityScheme(name = OpenApiConfig.BEARER_AUTH, type = SecuritySchemeType.HTTP, scheme = "bearer",
        bearerFormat = "JWT", description = "Il token che restituiscono la registrazione e il login, senza la parola Bearer. "
                + "Vale come una password per 24 ore e permette anche di eliminare l'account: usalo solo qui e prova con un account di prova.")
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";
}
