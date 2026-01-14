package com.ssg9th2team.geharbang.domain.ai_agent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation;
import com.ssg9th2team.geharbang.domain.accommodation.entity.ApprovalStatus;
import com.ssg9th2team.geharbang.domain.accommodation.repository.jpa.AccommodationJpaRepository;
import com.ssg9th2team.geharbang.domain.ai_agent.dto.AiAgentDto;
import com.ssg9th2team.geharbang.domain.ai_agent.entity.AgentChatMessage;
import com.ssg9th2team.geharbang.domain.ai_agent.entity.AgentChatRoom;
import com.ssg9th2team.geharbang.domain.ai_agent.repository.AgentChatMessageRepository;
import com.ssg9th2team.geharbang.domain.ai_agent.repository.AgentChatRoomRepository;
import com.ssg9th2team.geharbang.domain.auth.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiAgentService {

    private final AgentChatRoomRepository roomRepository;
    private final AgentChatMessageRepository messageRepository;
    private final AccommodationJpaRepository accommodationRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${GEMINI_API_KEY:}")
    private String geminiApiKey;

    @Value("${GEMINI_MODEL:gemini-1.5-flash}")
    private String geminiModel;

    @Value("${GEMINI_BASE_URL:https://generativelanguage.googleapis.com/v1beta}")
    private String geminiBaseUrl;

    private static final String SYSTEM_PROMPT = """
            당신은 제주도 게스트하우스/숙소 추천 전문 AI 어시스턴트 "지금이곳"입니다.

            역할:
            - 사용자와 친근하게 대화하며 원하는 숙소를 찾아줍니다
            - 위치, 테마, 가격, 인원수 등을 파악하여 맞춤 추천합니다
            - 필요한 정보가 부족하면 자연스럽게 질문합니다

            제주도 주요 지역: 애월, 함덕, 성산, 중문, 서귀포, 협재, 한림, 표선, 우도, 월정리, 김녕, 세화

            테마: 자연(NATURE), 분위기(VIBE), 액티비티(ACTIVITY), 파티(PARTY), 만남(MEETING),
                  반려동물(PERSONA), 시설(FACILITY), 맛집(FOOD), 문화(CULTURE), 놀이(PLAY)

            응답 규칙:
            1. 항상 친근하고 도움이 되는 톤으로 응답하세요
            2. 숙소 검색이 필요하면 [SEARCH] 태그 사용: [SEARCH:location=애월,theme=PARTY,maxPrice=100000]
            3. 숙소 추천 시 간단한 설명과 함께 추천 이유를 설명하세요
            4. 사용자가 더 자세한 정보를 원하면 제공하세요
            5. 예약이나 결제는 서비스 내에서 진행하도록 안내하세요
            """;

    private static final String WELCOME_MESSAGE = """
            안녕하세요! 저는 제주도 숙소 추천 AI "지금이곳"이에요 🏠✨

            어떤 여행을 계획하고 계세요?
            원하시는 지역, 분위기, 함께하는 인원 등을 알려주시면 딱 맞는 숙소를 찾아드릴게요!
            """;

    /**
     * 새 대화방 생성
     */
    @Transactional
    public Long createRoom(User user) {
        AgentChatRoom room = roomRepository.save(AgentChatRoom.builder()
                .user(user)
                .title("새 대화")
                .lastMessage(WELCOME_MESSAGE)
                .build());

        // 웰컴 메시지 저장
        messageRepository.save(AgentChatMessage.builder()
                .room(room)
                .role("model")
                .content(WELCOME_MESSAGE)
                .build());

        return room.getId();
    }

    /**
     * 대화방 목록 조회
     */
    @Transactional(readOnly = true)
    public List<AiAgentDto.ChatRoomResponse> getRooms(User user) {
        return roomRepository.findByUserOrderByUpdatedAtDesc(user).stream()
                .map(r -> AiAgentDto.ChatRoomResponse.builder()
                        .id(r.getId())
                        .title(r.getTitle())
                        .lastMessage(r.getLastMessage())
                        .updatedAt(r.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 특정 방의 메시지 조회
     */
    @Transactional(readOnly = true)
    public List<AiAgentDto.MessageResponse> getMessages(Long roomId, User user) {
        AgentChatRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("대화방을 찾을 수 없습니다."));

        if (!room.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }

        return messageRepository.findByRoomOrderByCreatedAtAsc(room).stream()
                .map(m -> AiAgentDto.MessageResponse.builder()
                        .id(m.getId())
                        .role(m.getRole())
                        .content(m.getContent())
                        .createdAt(m.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 메시지 전송 및 AI 응답
     */
    @Transactional
    public AiAgentDto.ChatResponse chat(Long roomId, String userMessage, User user) {
        AgentChatRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("대화방을 찾을 수 없습니다."));

        if (!room.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }

        // 사용자 메시지 저장
        messageRepository.save(AgentChatMessage.builder()
                .room(room)
                .role("user")
                .content(userMessage)
                .build());

        // 대화 기록 조회
        List<AgentChatMessage> history = messageRepository.findByRoomOrderByCreatedAtAsc(room);

        // Gemini API 호출
        String aiResponse;
        List<AiAgentDto.AccommodationSummary> recommendations = new ArrayList<>();

        try {
            aiResponse = callGeminiMultiTurn(history);

            // [SEARCH:...] 태그 처리
            if (aiResponse.contains("[SEARCH:")) {
                SearchParams params = parseSearchTag(aiResponse);
                List<Accommodation> results = searchAccommodations(params);
                recommendations = toSummaries(results);

                // 검색 결과를 응답에 포함
                aiResponse = aiResponse.replaceAll("\\[SEARCH:[^\\]]*\\]", "");
                if (!recommendations.isEmpty()) {
                    aiResponse += "\n\n추천 숙소를 찾았어요! 🎉";
                }
            }

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("Gemini API Error: Status={}, Body={}", e.getStatusCode(), e.getResponseBodyAsString());
            aiResponse = "죄송해요, AI 연결에 문제가 생겼어요. (Error: " + e.getStatusCode() + ")";
        } catch (Exception e) {
            log.error("AI 응답 생성 실패", e);
            aiResponse = "죄송해요, 일시적인 오류가 발생했어요. 다시 말씀해 주시겠어요?";
        }

        // AI 응답 저장
        String accommodationIds = recommendations.isEmpty() ? null
                : recommendations.stream()
                        .map(a -> String.valueOf(a.getId()))
                        .collect(Collectors.joining(","));

        messageRepository.save(AgentChatMessage.builder()
                .room(room)
                .role("model")
                .content(aiResponse)
                .accommodationIds(accommodationIds)
                .build());

        // 대화방 업데이트
        room.updateLastMessage(aiResponse);

        // 첫 대화면 제목 업데이트
        if (history.size() <= 2) {
            String title = userMessage.length() > 30 ? userMessage.substring(0, 30) + "..." : userMessage;
            room.updateTitle(title);
        }

        return AiAgentDto.ChatResponse.builder()
                .reply(aiResponse)
                .recommendedAccommodations(recommendations)
                .success(true)
                .build();
    }

    /**
     * 대화방 삭제
     */
    @Transactional
    public void deleteRoom(Long roomId, User user) {
        AgentChatRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("대화방을 찾을 수 없습니다."));

        if (!room.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("삭제 권한이 없습니다.");
        }

        messageRepository.deleteByRoom(room);
        roomRepository.delete(room);
    }

    /**
     * Gemini Multi-turn Chat API 호출
     */
    private String callGeminiMultiTurn(List<AgentChatMessage> history) throws JsonProcessingException {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            return "AI 서비스가 현재 사용 불가능합니다. 잠시 후 다시 시도해주세요.";
        }

        String url = String.format("%s/models/%s:generateContent?key=%s", geminiBaseUrl, geminiModel, geminiApiKey);

        // 대화 기록을 Gemini 형식으로 변환
        List<Map<String, Object>> contents = new ArrayList<>();

        // 시스템 프롬프트 추가 (첫 번째 user 메시지로)
        contents.add(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", SYSTEM_PROMPT + "\n\n위 지침을 이해했으면 '네, 이해했습니다'라고 답하세요."))));
        contents.add(Map.of(
                "role", "model",
                "parts", List.of(Map.of("text", "네, 이해했습니다. 제주도 숙소 추천 AI '지금이곳'으로서 친근하게 도움을 드리겠습니다."))));

        // 실제 대화 기록 추가
        for (AgentChatMessage msg : history) {
            contents.add(Map.of(
                    "role", msg.getRole(),
                    "parts", List.of(Map.of("text", msg.getContent()))));
        }

        Map<String, Object> body = Map.of(
                "contents", contents,
                "generationConfig", Map.of("temperature", 0.7, "maxOutputTokens", 1024));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        return parseGeminiResponse(response.getBody());
    }

    private String parseGeminiResponse(String responseBody) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");

        if (textNode.isMissingNode()) {
            throw new IllegalStateException("Gemini 응답에서 텍스트를 찾을 수 없습니다");
        }

        return textNode.asText().trim();
    }

    /**
     * [SEARCH:...] 태그 파싱
     */
    private SearchParams parseSearchTag(String response) {
        SearchParams params = new SearchParams();

        int start = response.indexOf("[SEARCH:");
        int end = response.indexOf("]", start);
        if (start >= 0 && end > start) {
            String searchStr = response.substring(start + 8, end);
            for (String pair : searchStr.split(",")) {
                String[] kv = pair.split("=");
                if (kv.length == 2) {
                    switch (kv[0].trim()) {
                        case "location" -> params.location = kv[1].trim();
                        case "theme" -> params.theme = kv[1].trim();
                        case "maxPrice" -> params.maxPrice = Integer.parseInt(kv[1].trim());
                        case "guests" -> params.guests = Integer.parseInt(kv[1].trim());
                    }
                }
            }
        }

        return params;
    }

    private static class SearchParams {
        String location;
        String theme;
        Integer maxPrice;
        Integer guests;
    }

    /**
     * 숙소 검색
     */
    private List<Accommodation> searchAccommodations(SearchParams params) {
        List<Accommodation> results = new ArrayList<>();

        // 위치 기반 검색
        if (params.location != null && !params.location.isBlank()) {
            results.addAll(accommodationRepository.findByLocation(params.location));
        }

        if (params.guests != null) {
            log.info("검색 조건 - 인원: {}명 (필터링 미적용)", params.guests);
        }

        // 테마 기반 검색 추가
        if (params.theme != null && !params.theme.isBlank()) {
            List<Accommodation> themeResults = accommodationRepository.findByThemeCategories(List.of(params.theme));
            if (results.isEmpty()) {
                results.addAll(themeResults);
            } else {
                // 교집합
                Set<Long> themeIds = themeResults.stream().map(Accommodation::getAccommodationsId)
                        .collect(Collectors.toSet());
                results = results.stream()
                        .filter(a -> themeIds.contains(a.getAccommodationsId()))
                        .collect(Collectors.toList());
            }
        }

        // 가격 필터링
        if (params.maxPrice != null) {
            results = results.stream()
                    .filter(a -> a.getMinPrice() != null && a.getMinPrice() <= params.maxPrice)
                    .collect(Collectors.toList());
        }

        // 승인된 숙소만, 평점순 정렬, 최대 5개
        return results.stream()
                .filter(a -> a.getApprovalStatus() == ApprovalStatus.APPROVED)
                .filter(a -> a.getAccommodationStatus() != null && a.getAccommodationStatus() == 1)
                .sorted((a, b) -> {
                    Double ratingA = a.getRating() != null ? a.getRating() : 0.0;
                    Double ratingB = b.getRating() != null ? b.getRating() : 0.0;
                    return ratingB.compareTo(ratingA);
                })
                .limit(5)
                .collect(Collectors.toList());
    }

    private final com.ssg9th2team.geharbang.domain.accommodation.repository.mybatis.AccommodationMapper accommodationMapper;

    // ... (Existing variables)

    private List<AiAgentDto.AccommodationSummary> toSummaries(List<Accommodation> accommodations) {
        return accommodations.stream()
                .map(a -> {
                    String thumbnailUrl = null;
                    try {
                        var fullInfo = accommodationMapper.selectAccommodationById(a.getAccommodationsId());
                        if (fullInfo != null) {
                            if (fullInfo.getMainImageUrl() != null) {
                                thumbnailUrl = fullInfo.getMainImageUrl();
                            } else if (fullInfo.getImages() != null && !fullInfo.getImages().isEmpty()) {
                                thumbnailUrl = fullInfo.getImages().get(0).getImageUrl();
                            }
                        }
                    } catch (Exception e) {
                        log.warn("이미지 조회 실패: {}", a.getAccommodationsId());
                    }

                    return AiAgentDto.AccommodationSummary.builder()
                            .id(a.getAccommodationsId())
                            .name(a.getAccommodationsName())
                            .city(a.getCity())
                            .district(a.getDistrict())
                            .rating(a.getRating())
                            .reviewCount(a.getReviewCount())
                            .minPrice(a.getMinPrice())
                            .thumbnailUrl(thumbnailUrl)
                            .build();
                })
                .collect(Collectors.toList());
    }
}
