package com.foureyes.moai.backend.domain.ai.internal;

import com.foureyes.moai.backend.commons.exception.CustomException;
import com.foureyes.moai.backend.commons.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ModelResolver {

    private final AiModelsProperties aiModelsProperties;

    /**
     * 입력: String requested
     * 출력: ModelEntry
     * 기능: 요청된 모델ID에 해당하는 설정(ModelEntry)을 조회합니다.
     */
    public ModelEntry resolveOption(String requested) {
        if (requested == null || requested.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        String key = requested.trim();
        return aiModelsProperties.getModels().stream()
            .filter(m -> m.getModelId().equalsIgnoreCase(key))
            .findFirst()
            .orElseThrow(() -> new CustomException(ErrorCode.INVALID_REQUEST));
    }
}
