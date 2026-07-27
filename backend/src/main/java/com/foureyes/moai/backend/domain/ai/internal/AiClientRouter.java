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

    /**
     * 입력: ModelResolver modelResolver, List<AiApiClient> clients
     * 출력: AiClientRouter
     * 기능: 등록된 AiApiClient들을 프로바이더명 기준으로 매핑합니다.
     */
    public AiClientRouter(ModelResolver modelResolver, List<AiApiClient> clients) {
        this.modelResolver = modelResolver;
        this.clientsByProvider = clients.stream()
            .collect(Collectors.toMap(AiApiClient::getProviderName, Function.identity()));
    }

    /**
     * 입력: String requestedModelKey, String prompt
     * 출력: Mono<String>
     * 기능: 요청 모델키에 해당하는 프로바이더 클라이언트를 찾아 AI 호출을 위임합니다.
     */
    public Mono<String> generateJsonArray(String requestedModelKey, String prompt) {
        ModelEntry option = modelResolver.resolveOption(requestedModelKey);
        AiApiClient client = clientsByProvider.get(option.getProvider());
        if (client == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        return client.generate(option.getModelId(), prompt);
    }
}