package com.foureyes.moai.backend.domain.ai.internal;

import com.foureyes.moai.backend.commons.exception.CustomException;
import com.foureyes.moai.backend.commons.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ModelResolver {

    private final AiModelsProperties aiModelsProperties;

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