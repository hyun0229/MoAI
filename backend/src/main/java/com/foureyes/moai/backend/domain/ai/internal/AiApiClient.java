package com.foureyes.moai.backend.domain.ai.internal;

import reactor.core.publisher.Mono;

public interface AiApiClient {

    /** 이 클라이언트가 담당하는 프로바이더 */
    String getProviderName();


    /** 모델ID와 프롬프트를 받아 JSON 배열 문자열을 반환 */
    Mono<String> generate(String modelId, String prompt);
}

