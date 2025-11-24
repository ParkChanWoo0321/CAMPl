// src/main/java/com/example/cample/place/service/PlaceGptClient.java
package com.example.cample.place.service;

import com.example.cample.place.dto.PlaceSummaryDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlaceGptClient {

    @Value("${app.openai.api-key}")
    private String apiKey;

    @Value("${app.openai.model:gpt-4.1-mini}")
    private String model;

    private static final String API_URL = "https://api.openai.com/v1/chat/completions";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * candidates 안에서만 place id를 고르게 한다.
     * 실패 시 빈 리스트 반환 (상위에서 fallback 처리).
     */
    public List<Long> choosePlaceIds(String scheduleType,
                                     String withWhom,
                                     List<PlaceSummaryDto> candidates,
                                     int limit) {
        if (candidates == null || candidates.isEmpty()) return List.of();
        if (limit <= 0) limit = 3;
        if (limit > candidates.size()) limit = candidates.size();

        try {
            // 1) 후보 목록을 단순 JSON으로 축소(id, name, type, address)
            List<Map<String, Object>> simplified = candidates.stream()
                    .map(c -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("id", c.getId());
                        m.put("name", c.getName());
                        m.put("type", c.getType() != null ? c.getType().name() : null);
                        m.put("address", c.getAddress());
                        return m;
                    })
                    .collect(Collectors.toList());

            String candidatesJson = objectMapper.writeValueAsString(simplified);

            // 2) 프롬프트
            String prompt = """
                    너는 사용자의 상황에 맞는 장소를 고르는 추천 엔진이다.
                    중요한 규칙:
                    - 반드시 '후보 목록' 안에 있는 장소(id 기준)에서만 골라야 한다.
                    - 새로운 장소를 상상해서 만들면 안 된다.
                    - 추천 개수만큼 id를 고르고, JSON 배열 형식으로만 응답해야 한다.
                    - 응답 예시: [1, 5, 9]
                    
                    입력 정보:
                    - 일정 타입: %s
                    - 함께 가는 사람: %s
                    - 추천 개수: %d
                    
                    아래는 후보 목록이다 (JSON 배열, 각 객체는 {id, name, type, address}):
                    %s
                    
                    위 후보 목록을 보고, 조건에 가장 잘 맞는 place id들을 골라라.
                    JSON 배열 이외의 다른 텍스트는 절대 포함하지 마라.
                    """.formatted(scheduleType, withWhom, limit, candidatesJson);

            // 3) Chat Completions 요청 body
            Map<String, Object> messageSys = Map.of(
                    "role", "system",
                    "content", "너는 JSON 형식만 반환하는 장소 추천 엔진이다."
            );
            Map<String, Object> messageUser = Map.of(
                    "role", "user",
                    "content", prompt
            );

            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("messages", List.of(messageSys, messageUser));
            body.put("temperature", 0.4);

            // 4) HTTP 호출
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            Map<String, Object> resp = restTemplate.postForObject(
                    API_URL, entity, Map.class);

            if (resp == null) return List.of();

            // 5) 응답에서 content 추출
            List<Map<String, Object>> choices =
                    (List<Map<String, Object>>) resp.get("choices");
            if (choices == null || choices.isEmpty()) return List.of();

            Map<String, Object> first = choices.get(0);
            Map<String, Object> message =
                    (Map<String, Object>) first.get("message");
            if (message == null) return List.of();

            String content = (String) message.get("content");
            if (content == null || content.isBlank()) return List.of();

            // 6) content를 JSON 배열로 파싱 (예: [1, 5, 9])
            List<Long> ids = objectMapper.readValue(
                    content,
                    new TypeReference<List<Long>>() {}
            );

            // 후보에 실제로 존재하는 id만 남기기
            Set<Long> candidateIds = candidates.stream()
                    .map(PlaceSummaryDto::getId)
                    .collect(Collectors.toSet());

            List<Long> filtered = ids.stream()
                    .filter(candidateIds::contains)
                    .limit(limit)
                    .collect(Collectors.toList());

            return filtered;
        } catch (Exception e) {
            log.warn("PlaceGptClient.choosePlaceIds 실패, fallback 사용 예정", e);
            return List.of();
        }
    }
}
