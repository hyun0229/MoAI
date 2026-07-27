package com.foureyes.moai.backend.domain.session.service;

import io.livekit.server.AccessToken;
import io.livekit.server.CanPublish;
import io.livekit.server.CanSubscribe;
import io.livekit.server.RoomJoin;
import io.livekit.server.RoomName;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LiveKitTokenGenerator {

    @Value("${livekit.api.key}")    private String apiKey;
    @Value("${livekit.api.secret}") private String apiSecret;
    @Value("${livekit.ws-url}")     private String wsUrl;

    /**
     * 입력: String roomName, int userId, String displayName
     * 출력: String(JWT 토큰)
     * 기능: LiveKit 방 참가용 Access Token을 생성합니다.
     */
    public String createJoinToken(String roomName, int userId, String displayName) {
        String identity = Integer.toString(userId);
        String name = (displayName != null && !displayName.isBlank()) ? displayName : identity;

        AccessToken token = new AccessToken(apiKey, apiSecret);
        token.setIdentity(identity);
        token.setName(name);
        token.addGrants(
            new RoomJoin(true),
            new RoomName(roomName),
            new CanPublish(true),
            new CanSubscribe(true)
        );

        return token.toJwt();
    }

    public String getWsUrl() { return wsUrl; }
}
