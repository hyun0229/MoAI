package com.foureyes.moai.backend.domain.ai.internal;

import reactor.core.publisher.Mono;

/**
 * AI 프로바이더별 API 클라이언트가 구현해야 하는 공통 인터페이스.
 * 새 프로바이더 추가 시 이 인터페이스만 구현하면 되고,
 * AiClientRouter의 기존 코드는 수정할 필요가 없다 (OCP).
 */
public interface AiApiClient {

    /** 이 클라이언트가 담당하는 프로바이더 */
    String getProviderName();


    /** 모델ID와 프롬프트를 받아 JSON 배열 문자열을 반환 */
    Mono<String> generate(String modelId, String prompt);
}
