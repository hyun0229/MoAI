package com.foureyes.moai.backend.domain.ai.internal;


import lombok.Getter;

@Getter
public enum ModelOption {

    // GEMINI_20_FLASH_LITE("gemini-2.0-flash-lite", AiProvider.GEMINI),
    GEMINI_20_FLASH("gemini-2.0-flash", AiProvider.GEMINI),
    GEMINI_35_FLASH_LITE("gemini-3.5-flash-lite", AiProvider.GEMINI),
    GEMINI_36_FLASH("gemini-3.6-flash", AiProvider.GEMINI),
    GPT_4O("gpt-4o", AiProvider.OPENAI),
    GPT_4O_MINI("gpt-4o-mini", AiProvider.OPENAI);

    private final String modelId;
    private final AiProvider provider;

    ModelOption(String modelId, AiProvider provider) {
        this.modelId = modelId;
        this.provider = provider;
    }

    /** 키(ENUM명 or 실제 모델ID)로 찾기 */
    public static ModelOption fromKey(String key) {
        for (ModelOption m : values()) {
            if (m.name().equalsIgnoreCase(key) || m.modelId.equalsIgnoreCase(key)) return m;
        }
        throw new IllegalArgumentException("Unknown model key: " + key);
    }
}
