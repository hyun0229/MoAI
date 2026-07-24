package com.foureyes.moai.backend.domain.ai.internal;

import com.foureyes.moai.backend.commons.exception.CustomException;
import com.foureyes.moai.backend.commons.exception.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class ModelResolver {

    /**
     * 프론트에서 넘어온 modelType이 없거나 잘못되면 즉시 400.
     * (기존처럼 기본값 사용하지 않음)
     */
    public ModelOption resolveOption(String requested) {
        if (requested == null || requested.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST); // 필요하면 INVALID_MODEL_TYPE로 분리
        }
        try {
            return ModelOption.fromKey(requested.trim()); // enum명 또는 실제 modelId 허용
        } catch (IllegalArgumentException ex) {
            // fromKey에서 못 찾으면 400
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }

}
