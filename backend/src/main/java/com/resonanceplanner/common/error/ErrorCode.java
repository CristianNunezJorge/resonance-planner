package com.resonanceplanner.common.error;

import org.springframework.http.HttpStatus;

/** The error codes of docs/05-api.md, each with its HTTP status and default message in Italian. */
public enum ErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Alcuni campi non sono validi."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Devi effettuare l'accesso."),
    BAD_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Email o password non corrette."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Non hai i permessi per questa operazione."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Risorsa non trovata."),
    CONFLICT(HttpStatus.CONFLICT, "L'operazione non è possibile nello stato attuale."),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Troppe richieste: riprova più tardi."),
    AGENT_DAILY_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "Hai raggiunto il limite giornaliero di messaggi all'assistente."),
    AGENT_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "L'assistente non è disponibile in questo momento."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Si è verificato un errore imprevisto. Riprova più tardi.");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus status() {
        return status;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
