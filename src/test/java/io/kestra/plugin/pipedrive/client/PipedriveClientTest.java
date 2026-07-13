package io.kestra.plugin.pipedrive.client;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.pipedrive.models.PipedriveResponse;

import jakarta.inject.Inject;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@KestraTest
class PipedriveClientTest {
    @Inject
    private RunContextFactory runContextFactory;

    private MockWebServer mockWebServer;

    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    private String baseUrl() {
        return mockWebServer.url("/v2").toString().replaceAll("/$", "");
    }

    @Test
    void shouldAppendEncodedQueryParams() throws Exception {
        mockWebServer.enqueue(
            new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"success\": true, \"data\": []}")
        );

        RunContext runContext = runContextFactory.of();

        Map<String, String> queryParams = new LinkedHashMap<>();
        queryParams.put("status", "open won");
        queryParams.put("limit", "10");

        try (PipedriveClient client = new PipedriveClient(runContext, "token", baseUrl())) {
            PipedriveResponse<List<Object>> response = client.get(
                "/deals", queryParams,
                new TypeReference<>() {
                }
            );

            assertThat(response.getSuccess(), is(true));
        }

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getPath(), is("/v2/deals?status=open+won&limit=10"));
    }

    @Test
    void shouldNotAppendQuestionMarkWhenNoQueryParams() throws Exception {
        mockWebServer.enqueue(
            new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"success\": true, \"data\": []}")
        );

        RunContext runContext = runContextFactory.of();

        try (PipedriveClient client = new PipedriveClient(runContext, "token", baseUrl())) {
            PipedriveResponse<List<Object>> response = client.get(
                "/deals",
                new TypeReference<>() {
                }
            );

            assertThat(response.getSuccess(), is(true));
        }

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getPath(), is("/v2/deals"));
    }
}
