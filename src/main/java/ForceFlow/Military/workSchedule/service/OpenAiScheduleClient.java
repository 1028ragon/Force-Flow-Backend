package ForceFlow.Military.workSchedule.service;

import ForceFlow.Military.dto.responseDto.AiModelResponse;
import ForceFlow.Military.workSchedule.dto.internal.AiInternalRequest;
import ForceFlow.Military.workSchedule.exception.OpenAiScheduleClientException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class OpenAiScheduleClient {

    private static final String CHAT_COMPLETIONS_URL = "https://api.openai.com/v1/chat/completions";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String apiKey;
    private final String model;

    public OpenAiScheduleClient(
            ObjectMapper objectMapper,
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.model:gpt-4.1-mini}") String model
    ) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
    }

    public AiModelResponse recommend(AiInternalRequest internalRequest) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new OpenAiScheduleClientException("OpenAI API key가 설정되지 않았습니다.");
        }

        try {
            String requestJson = objectMapper.writeValueAsString(internalRequest);
            OpenAiChatRequest chatRequest = new OpenAiChatRequest(
                    model,
                    List.of(
                            new OpenAiMessage("system", systemPrompt()),
                            new OpenAiMessage("user", requestJson)
                    ),
                    0.2,
                    new OpenAiResponseFormat("json_object")
            );
            String responseBody = send(chatRequest);
            String content = extractContent(responseBody);
            return objectMapper.readValue(normalizeJsonContent(content), AiModelResponse.class);
        } catch (OpenAiScheduleClientException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new OpenAiScheduleClientException("OpenAI 요청 또는 응답 JSON 처리에 실패했습니다.", exception);
        }
    }

    private String send(OpenAiChatRequest chatRequest) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(CHAT_COMPLETIONS_URL))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            objectMapper.writeValueAsString(chatRequest),
                            StandardCharsets.UTF_8
                    ))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new OpenAiScheduleClientException("OpenAI API 호출에 실패했습니다. status=" + response.statusCode());
            }

            return response.body();
        } catch (IOException exception) {
            throw new OpenAiScheduleClientException("OpenAI API 통신 중 오류가 발생했습니다.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new OpenAiScheduleClientException("OpenAI API 호출이 중단되었습니다.", exception);
        }
    }

    private String extractContent(String responseBody) throws Exception {
        OpenAiChatResponse response = objectMapper.readValue(responseBody, OpenAiChatResponse.class);
        if (response.choices() == null || response.choices().isEmpty()) {
            throw new OpenAiScheduleClientException("OpenAI 응답에 choices가 없습니다.");
        }

        OpenAiMessage message = response.choices().get(0).message();
        if (message == null || message.content() == null || message.content().isBlank()) {
            throw new OpenAiScheduleClientException("OpenAI 응답 content가 비어 있습니다.");
        }

        return message.content();
    }

    private String normalizeJsonContent(String content) {
        String normalized = content.trim();
        if (normalized.startsWith("```json")) {
            normalized = normalized.substring(7).trim();
        } else if (normalized.startsWith("```")) {
            normalized = normalized.substring(3).trim();
        }
        if (normalized.endsWith("```")) {
            normalized = normalized.substring(0, normalized.length() - 3).trim();
        }

        return normalized;
    }

    private String systemPrompt() {
        return """
                당신은 군 부대 근무표 추천 AI입니다.
                반드시 유효한 JSON만 반환하세요.
                markdown, 설명문, 코드블록은 포함하지 마세요.
                soldiers 배열에서 eligible이 true인 인원만 추천하세요.
                duty.requiredCount와 정확히 같은 수의 인원을 추천하세요.
                공정성 우선순위는 recentDutyCount가 낮은 인원, workedYesterday가 false인 인원,
                hasScheduleConflict가 false인 인원, 제외 상태가 아닌 인원입니다.
                각 reason은 간결한 한국어 문장으로 작성하세요.
                반환 형식은 {"recommendedUsers":[{"userId":1,"score":92,"reason":"..."}],"warningMessage":null} 입니다.
                """;
    }

    private record OpenAiChatRequest(
            String model,
            List<OpenAiMessage> messages,
            Double temperature,
            @JsonProperty("response_format") OpenAiResponseFormat responseFormat
    ) {
    }

    private record OpenAiMessage(
            String role,
            String content
    ) {
    }

    private record OpenAiResponseFormat(
            String type
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OpenAiChatResponse(
            List<OpenAiChoice> choices
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OpenAiChoice(
            OpenAiMessage message
    ) {
    }
}
