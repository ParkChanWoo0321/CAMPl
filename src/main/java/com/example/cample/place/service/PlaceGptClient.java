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
                    너는 대학 캠퍼스 주변의 식당/카페/술집을 추천하는 엔진이다.

                    [후보 데이터 설명]
                    - 각 장소 객체는 {id, name, type, address} 형식이다.
                    - type은 "RESTAURANT"(식당), "CAFE"(카페), "BAR"(술집) 중 하나이다.

                    [반드시 지켜야 할 규칙]
                    1. 반드시 '후보 목록' 안에 있는 장소(id 기준)에서만 골라라.
                       새로운 장소를 상상해서 만들면 안 된다.
                    2. 추천 개수만큼 id만 골라서 JSON 배열 형식으로만 응답해야 한다.
                       예시: [1, 5, 9]
                    3. 아래의 일정 타입 / 함께 가는 사람 규칙을 따라 type을 선택하라.

                    [일정 타입별 기본 우선순위]
                    - "팀플", "과제", "미팅":
                      * 공부/회의 목적이므로 CAFE 타입을 최우선으로 고른다.
                      * RESTAURANT는 필요할 때만 선택하고 BAR는 절대 선택하지 마라.
                    - "식사":
                      * RESTAURANT 타입을 최우선으로 고른다.
                      * 적절한 RESTAURANT가 부족하면 CAFE를 보조로 선택할 수 있다.
                    - "휴식":
                      * 기본적으로 CAFE를 우선 추천한다.
                      * 동기/선배/후배와 함께라면 BAR도 선택할 수 있다.

                    [함께 가는 사람 규칙]
                    - "교수님":
                      * BAR는 절대 선택하지 말고, CAFE 또는 RESTAURANT만 선택하라.
                    - "혼자":
                      * 팀플/과제/미팅/휴식이라면 조용한 CAFE(type=CAFE)를 우선 고려하라.

                    4. 같은 조건으로 여러 번 호출되더라도 항상 같은 조합만 나오지 않도록,
                       후보들 중에서 다양하게 조합을 만들도록 노력하라.

                    [입력 정보]
                    - 일정 타입: %s
                    - 함께 가는 사람: %s
                    - 추천 개수: %d

                    아래는 후보 목록이다 (JSON 배열, 각 객체는 {id, name, type, address}):
                    %s

                    위 규칙을 모두 고려하여 조건에 가장 잘 맞는 place id들을 골라라.
                    반드시 JSON 배열 하나만 응답하고, 다른 텍스트나 설명은 절대 포함하지 마라.
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
            // 조금 더 다양하게 추천되도록 온도 조정
            body.put("temperature", 0.7);

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
