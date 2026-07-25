package com.foureyes.moai.backend.domain.ai.internal;

import com.foureyes.moai.backend.commons.exception.CustomException;
import com.foureyes.moai.backend.commons.exception.ErrorCode;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AiClientRouter {

    private final ModelResolver modelResolver;
    private final Map<String, AiApiClient> clientsByProvider;

    public AiClientRouter(ModelResolver modelResolver, List<AiApiClient> clients) {
        this.modelResolver = modelResolver;
        this.clientsByProvider = clients.stream()
            .collect(Collectors.toMap(AiApiClient::getProviderName, Function.identity()));
    }

    public Mono<String> generateJsonArray(String requestedModelKey, String prompt) {
        ModelEntry option = modelResolver.resolveOption(requestedModelKey);
        AiApiClient client = clientsByProvider.get(option.getProvider());
        if (client == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        return client.generate(option.getModelId(), prompt);
    }
}