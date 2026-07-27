// domain/study/session/service/LiveKitAdminClient.java
package com.foureyes.moai.backend.domain.session.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LiveKitAdminClient {

    @Value("${livekit.ws-url}") private String wsUrl;     // 필요 시 노출용
    @Value("${livekit.api.key}") private String apiKey;
    @Value("${livekit.api.secret}") private String apiSecret;

    /**
     * 입력: String roomName
     * 출력: void
     * 기능: LiveKit 방이 존재하도록 보장합니다.
     */
    public void ensureRoom(String roomName) {

    }
}
