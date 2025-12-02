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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

@KestraTest
class UpdateTest {
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
    void shouldUpdateDeal() throws Exception {
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody("""
                {
                  "success": true,
                  "data": {
                    "id": 987,
                    "status": "won",
                    "update_time": "2024-02-01T00:00:00Z"
                  }
                }
                """));

        RunContext runContext = runContextFactory.of();

        Update task = Update.builder()
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(baseUrl()))
            .dealId(Property.ofValue(987))
            .status(Property.ofValue("won"))
            .value(Property.ofValue(BigDecimal.valueOf(25000)))
            .build();

        Update.Output output = task.run(runContext);

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        String body = recordedRequest.getBody().readUtf8();

        assertThat(recordedRequest.getMethod(), is("PUT"));
        assertThat(recordedRequest.getPath(), containsString("/deals/987?api_token=token"));
        assertThat(body, containsString("won"));
        assertThat(body, containsString("25000"));

        assertThat(output.getDealId(), is(987));
        assertThat(output.getUpdateTime(), is("2024-02-01T00:00:00Z"));
    }

    @Test
    void shouldRequireAtLeastOneField() {
        RunContext runContext = runContextFactory.of();

        Update task = Update.builder()
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(baseUrl()))
            .dealId(Property.ofValue(12))
            .build();

        assertThrows(IllegalArgumentException.class, () -> task.run(runContext));
    }
}
