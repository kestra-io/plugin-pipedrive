package io.kestra.plugin.pipedrive.deals;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import jakarta.inject.Inject;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

@KestraTest
class CreateTest {
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
    void shouldCreateDeal() throws Exception {
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody("""
                {
                  "success": true,
                  "data": {
                    "id": 321,
                    "status": "open",
                    "add_time": "2024-01-01T00:00:00Z",
                    "update_time": "2024-01-01T00:00:00Z"
                  }
                }
                """));

        RunContext runContext = runContextFactory.of();

        Create task = Create.builder()
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(baseUrl()))
            .title(Property.ofValue("Test Deal"))
            .value(Property.ofValue(BigDecimal.valueOf(1000)))
            .currency(Property.ofValue("EUR"))
            .stageId(Property.ofValue(2))
            .build();

        Create.Output output = task.run(runContext);

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        String body = recordedRequest.getBody().readUtf8();

        assertThat(recordedRequest.getMethod(), is("POST"));
        assertThat(recordedRequest.getPath(), containsString("/deals?api_token=token"));
        assertThat(body, allOf(containsString("Test Deal"), containsString("EUR"), containsString("stage_id")));

        assertThat(output.getDealId(), is(321));
        assertThat(output.getAddTime(), is("2024-01-01T00:00:00Z"));
    }
}
