package com.foureyes.moai.backend.domain.session.service;

import jakarta.annotation.PostConstruct;

import io.livekit.server.RoomServiceClient;
import livekit.LivekitModels;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// LiveKit SDK import (0.10.x 기준)
// import io.livekit.server.RoomServiceClient;
// import livekit.LivekitModels; // SDK가 노출하는 Participant 모델 패키지명은 버전에 따라 달라질 수 있음

@Slf4j
@Component
@RequiredArgsConstructor
public class LiveKitRoomClient {

    @Value("${livekit.ws-url}")
    private String wsUrl;

    @Value("${livekit.api.key}")
    private String apiKey;

    @Value("${livekit.api.secret}")
    private String apiSecret;

    // private final RoomServiceClient client = new RoomServiceClient("http://localhost:7880", apiKey, apiSecret);
    // ↑ 실제 구현은 SDK 버전에 맞춰 RoomServiceClient 초기화. 예시는 생략(프로젝트에 맞춰 주입/생성).
    private RoomServiceClient client;

    /**
     * 입력: 없음
     * 출력: void
     * 기능: ws(s) URL을 http(s)로 변환하여 RoomServiceClient를 초기화합니다.
     */
    @PostConstruct
    void init() {
        // ws(s) → http(s)로 변환 (RoomServiceClient는 HTTP/HTTPS 엔드포인트 필요)
        String httpBase = wsUrl.startsWith("wss://")
            ? "https://" + wsUrl.substring("wss://".length())
            : wsUrl.startsWith("ws://")
            ? "http://" + wsUrl.substring("ws://".length())
            : wsUrl; // 이미 http(s)면 그대로

        // 예: http://localhost:7880
        this.client = RoomServiceClient.Companion.createClient(httpBase, apiKey, apiSecret);
        log.info("LiveKit RoomServiceClient initialized with {}", httpBase);
    }

    /** 우리가 쓰기 쉬운 POJO */
    public static class RoomParticipant {
        private final String identity; // 토큰의 identity (우리 규칙: userId 문자열)
        private final String name;     // 토큰의 name (표시명)
        public RoomParticipant(String identity, String name) {
            this.identity = identity;
            this.name = name;
        }
        public String getIdentity() { return identity; }
        public String getName() { return name; }
    }

    /** 실제 LiveKit 서버에서 현재 참가자 목록 조회 */
    public List<RoomParticipant> listParticipants(String roomName) {
        try {
            Response<List<LivekitModels.ParticipantInfo>> resp =
                client.listParticipants(roomName).execute(); // 동기 호출

            if (!resp.isSuccessful()) {
                String errMsg = null;
                // ✅ errorBody는 Closeable — try-with-resources로 안전하게 닫기
                try (ResponseBody eb = resp.errorBody()) {
                    errMsg = (eb != null) ? eb.string() : null;
                } catch (Exception ignore) { /* 로깅만 유지 */ }

                log.warn("LiveKit listParticipants unsuccessful. code={}, msg={}",
                    resp.code(), errMsg);
                return Collections.emptyList();
            }

            List<LivekitModels.ParticipantInfo> body = resp.body();
            if (body == null) {
                log.warn("LiveKit listParticipants: response body is null (code={})", resp.code());
                return Collections.emptyList();
            }

            List<RoomParticipant> out = new ArrayList<>(body.size());
            for (LivekitModels.ParticipantInfo p : body) {
                out.add(new RoomParticipant(p.getIdentity(), p.getName()));
            }
            return out;

        } catch (Exception e) {
            log.warn("LiveKit listParticipants failed for room {}: {}", roomName, e.toString());
            return Collections.emptyList();
        }
    }
}
