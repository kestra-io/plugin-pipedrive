package io.kestra.plugin.pipedrive.deals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;

import jakarta.inject.Inject;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;

@KestraTest
class DeleteTest {
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
    void shouldDeleteDeal() throws Exception {
        mockWebServer.enqueue(
            new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                    {
                      "success": true,
                      "data": {
                        "id": 123
                      }
                    }
                    """)
        );

        RunContext runContext = runContextFactory.of();

        Delete task = Delete.builder()
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(baseUrl()))
            .dealId(Property.ofValue(123))
            .build();

        Delete.Output output = task.run(runContext);

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getMethod(), is("DELETE"));
        assertThat(recordedRequest.getPath(), is("/v2/deals/123"));
        assertThat(recordedRequest.getPath(), not(containsString("api_token")));
        assertThat(recordedRequest.getHeader("x-api-token"), is("token"));

        assertThat(output.getId(), is(123));
        assertThat(output.getDeleted(), is(true));
    }

    @Test
    void shouldThrowWhenDeleteFails() {
        mockWebServer.enqueue(
            new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                    {
                      "success": false,
                      "error": "Deal not found"
                    }
                    """)
        );

        RunContext runContext = runContextFactory.of();

        Delete task = Delete.builder()
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(baseUrl()))
            .dealId(Property.ofValue(999))
            .build();

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> task.run(runContext));
        assertThat(exception.getMessage(), containsString("Deal not found"));
    }
}
