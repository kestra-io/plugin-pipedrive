package io.kestra.plugin.pipedrive.client;

import com.fasterxml.jackson.core.type.TypeReference;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.http.HttpRequest;
import io.kestra.core.http.HttpResponse;
import io.kestra.core.http.client.HttpClient;
import io.kestra.core.http.client.HttpClientException;
import io.kestra.core.http.client.configurations.HttpConfiguration;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.JacksonMapper;
import io.kestra.plugin.pipedrive.models.PipedriveResponse;
import org.slf4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpHeaders;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class PipedriveClient implements Closeable {
    public static final String DEFAULT_BASE_URL = "https://api.pipedrive.com/api/v2";

    private final HttpClient httpClient;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private final String apiToken;
    private final Logger logger;
    private final String baseUrl;

    public PipedriveClient(RunContext runContext, String apiToken, String baseUrl)
        throws IllegalVariableEvaluationException {

        this.apiToken = apiToken;
        this.logger = runContext.logger();

        String resolvedBaseUrl =
            (baseUrl == null || baseUrl.isBlank())
                ? DEFAULT_BASE_URL
                : baseUrl;

        this.baseUrl = resolvedBaseUrl.endsWith("/")
            ? resolvedBaseUrl.substring(0, resolvedBaseUrl.length() - 1)
            : resolvedBaseUrl;

        this.httpClient = HttpClient.builder()
            .runContext(runContext)
            .configuration(HttpConfiguration.builder().build())
            .build();

        this.objectMapper = JacksonMapper.ofJson().copy();
    }

    public <T> PipedriveResponse<T> get(String endpoint, TypeReference<PipedriveResponse<T>> typeRef) throws IOException {
        String url = buildUrl(endpoint);

        HttpRequest request = HttpRequest.builder()
            .uri(URI.create(url))
            .method("GET")
            .build();

        return executeRequest(request, typeRef);
    }

    public <T> PipedriveResponse<T> post(String endpoint, Object body, TypeReference<PipedriveResponse<T>> typeRef) throws IOException {
        String url = buildUrl(endpoint);
        String jsonBody = objectMapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.builder()
            .uri(URI.create(url))
            .method("POST")
            .headers(HttpHeaders.of(Map.of("Content-Type", List.of("application/json")), (s1, s2) -> true))
            .body(HttpRequest.StringRequestBody.builder()
                .contentType("application/json")
                .charset(StandardCharsets.UTF_8)
                .content(jsonBody)
                .build()
            )
            .build();

        return executeRequest(request, typeRef);
    }

    public <T> PipedriveResponse<T> put(String endpoint, Object body, TypeReference<PipedriveResponse<T>> typeRef) throws IOException {
        String url = buildUrl(endpoint);
        String jsonBody = objectMapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.builder()
            .uri(URI.create(url))
            .method("PUT")
            .headers(HttpHeaders.of(Map.of("Content-Type", List.of("application/json")), (s1, s2) -> true))
            .body(HttpRequest.StringRequestBody.builder()
                .contentType("application/json")
                .charset(StandardCharsets.UTF_8)
                .content(jsonBody)
                .build()
            )
            .build();

        return executeRequest(request, typeRef);
    }

    public <T> PipedriveResponse<T> delete(String endpoint, TypeReference<PipedriveResponse<T>> typeRef) throws IOException {
        String url = buildUrl(endpoint);

        HttpRequest request = HttpRequest.builder()
            .uri(URI.create(url))
            .method("DELETE")
            .build();

        return executeRequest(request, typeRef);
    }

    private <T> PipedriveResponse<T> executeRequest(HttpRequest request, TypeReference<PipedriveResponse<T>> typeRef) throws IOException {
        logger.debug("Executing Pipedrive API request: {} {}", request.getMethod(), request.getUri());

        try {
            HttpResponse<String> response = httpClient.request(request, String.class);

            String responseBody = response.getBody();

            if (response.getStatus().getCode() < 200 || response.getStatus().getCode() >= 300) {
                logger.error("Pipedrive API request failed: {} - {}", response.getStatus().getCode(), responseBody);
                throw new IOException("Pipedrive API request failed: " +
                    response.getStatus().getCode() + " - " + responseBody);
            }

            logger.debug("Pipedrive API response: {}", responseBody);
            return objectMapper.readValue(responseBody, typeRef);

        } catch (HttpClientException e) {
            throw new IOException("Pipedrive API request failed: " + e.getMessage(), e);
        } catch (IllegalVariableEvaluationException e) {
            throw new IOException("Failed to evaluate variables in HTTP request", e);
        }
    }

    private String buildUrl(String endpoint) {
        String separator = endpoint.contains("?") ? "&" : "?";
        return baseUrl + endpoint + separator + "api_token=" + apiToken;
    }

    @Override
    public void close() throws IOException {
        httpClient.close();
    }
}
