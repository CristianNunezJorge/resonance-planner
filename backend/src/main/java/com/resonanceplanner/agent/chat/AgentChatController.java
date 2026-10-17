package com.resonanceplanner.agent.chat;

import com.resonanceplanner.agent.chat.dto.AgentAnswerResponse;
import com.resonanceplanner.agent.chat.dto.AgentMessagePage;
import com.resonanceplanner.agent.chat.dto.AgentMessageRequest;
import com.resonanceplanner.agent.chat.dto.AgentMessageResponse;
import com.resonanceplanner.agent.chat.dto.AgentUsageResponse;
import com.resonanceplanner.auth.AuthenticatedUser;
import com.resonanceplanner.common.api.ApiResponse;
import com.resonanceplanner.common.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The chat of the logged user with the agent: no user in the URL, it is the one of the token (D44). */
@RestController
@RequestMapping("/api/me/agent")
@Tag(name = "Agente", description = "Chat con l'assistente: messaggi salvati, risposta con l'eventuale proposta e limiti giornalieri.")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class AgentChatController {

    private static final String WRONG_LIMIT = "Il limite va da 1 a 50.";

    private final AgentChatService chatService;
    private final AgentMessageService messageService;
    private final AgentQuota quota;

    public AgentChatController(AgentChatService chatService, AgentMessageService messageService, AgentQuota quota) {
        this.chatService = chatService;
        this.messageService = messageService;
        this.quota = quota;
    }

    @GetMapping("/messages")
    @Operation(summary = "Storico della chat", description = "I messaggi dal più recente, `limit` per pagina (da 1 a 50, 20 se manca). Per la pagina successiva si passa come `before` l'id del messaggio più vecchio ricevuto; `meta.hasMore` dice se ce ne sono altri.")
    public ApiResponse<List<AgentMessageResponse>> messages(@AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) @Positive(message = "Indica l'id di un messaggio.") Long before,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = WRONG_LIMIT) @Max(value = 50, message = WRONG_LIMIT)
            int limit) {
        AgentMessagePage page = messageService.page(user.id(), before, limit);
        return ApiResponse.page(page.messages(), page.meta());
    }

    @PostMapping("/messages")
    @Operation(summary = "Scrivi all'assistente", description = "Invia un messaggio (da 1 a 1000 caratteri) e aspetta la risposta, che può richiedere fino a qualche minuto. Il messaggio va a un modello gratuito di terzi tramite OpenRouter. 429 al limite giornaliero tuo o di tutta l'app, o con un messaggio già in attesa di risposta; 502 se l'assistente non risponde: in quel caso il messaggio non viene salvato e non conta.")
    public ApiResponse<AgentAnswerResponse> send(@AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody AgentMessageRequest request) {
        return ApiResponse.success(chatService.send(user.id(), request.content()));
    }

    @GetMapping("/usage")
    @Operation(summary = "Messaggi usati oggi", description = "Messaggi scritti oggi, limite giornaliero e orario di azzeramento (mezzanotte italiana). `callsLimitReached` è vero quando hai finito le tue richieste al modello di oggi (contano anche i messaggi non riusciti), `appLimitReached` quando le ha finite tutta l'app; entrambi ripartono a mezzanotte UTC.")
    public ApiResponse<AgentUsageResponse> usage(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(quota.usage(user.id()));
    }
}
