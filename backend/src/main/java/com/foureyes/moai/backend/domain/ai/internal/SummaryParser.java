package com.foureyes.moai.backend.domain.ai.internal;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foureyes.moai.backend.domain.ai.dto.SummaryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SummaryParser {

    private final ObjectMapper objectMapper;

    /** JSON → List<SummaryDto> (실패 시 [ ] 구간만 추출해서 1회 복구 시도) */
    public List<SummaryDto> parse(String jsonResponse) {
        try {
            return doParse(jsonResponse);
        } catch (Exception firstError) {
            log.warn("1차 JSON 파싱 실패, 배열 구간 추출 후 재시도: {}", firstError.getMessage());
            String repaired = extractJsonArray(jsonResponse);
            if (repaired == null) {
                log.error("AI 응답 JSON 파싱 실패 (복구 불가): {}", jsonResponse, firstError);
                throw new RuntimeException("AI 응답 파싱에 실패했습니다.", firstError);
            }
            try {
                return doParse(repaired);
            } catch (Exception secondError) {
                log.error("AI 응답 JSON 파싱 실패 (복구 시도 후에도 실패): {}", jsonResponse, secondError);
                throw new RuntimeException("AI 응답 파싱에 실패했습니다.", secondError);
            }
        }
    }

    /**
     * 입력: String json
     * 출력: List<SummaryDto>
     * 기능: JSON 문자열을 SummaryDto 리스트로 역직렬화합니다.
     */
    private List<SummaryDto> doParse(String json) throws Exception {
        List<SummaryDto> list = objectMapper.readValue(json, new TypeReference<>() {});
        log.info("AI 응답 파싱 완료 ({} items)", list.size());
        return list;
    }

    /** 응답 문자열에서 첫 '[' ~ 마지막 ']' 구간만 잘라냄 (마크다운/설명 텍스트가 섞였을 때 대비) */
    private String extractJsonArray(String raw) {
        if (raw == null) return null;
        int start = raw.indexOf('[');
        int end = raw.lastIndexOf(']');
        if (start < 0 || end < 0 || end < start) return null;
        return raw.substring(start, end + 1);
    }
}