package io.kestra.plugin.pipedrive.persons;

import java.util.List;
import java.util.Map;

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
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
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
    void shouldUpdatePerson() throws Exception {
        mockWebServer.enqueue(
            new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                    {
                      "success": true,
                      "data": {
                        "id": 55,
                        "name": "Jane Doe",
                        "update_time": "2024-04-01T00:00:00Z"
                      }
                    }
                    """)
        );

        RunContext runContext = runContextFactory.of();

        Update task = Update.builder()
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(baseUrl()))
            .personId(Property.ofValue(55))
            .orgId(Property.ofValue(42))
            .emails(
                Property.ofValue(
                    List.of(
                        Map.of("value", "jane.doe@newcompany.com", "primary", true)
                    )
                )
            )
            .build();

        Update.Output output = task.run(runContext);

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        String body = recordedRequest.getBody().readUtf8();

        assertThat(recordedRequest.getMethod(), is("PUT"));
        assertThat(recordedRequest.getPath(), is("/v2/persons/55"));
        assertThat(recordedRequest.getPath(), not(containsString("api_token")));
        assertThat(recordedRequest.getHeader("x-api-token"), is("token"));
        assertThat(body, allOf(containsString("org_id"), containsString("jane.doe@newcompany.com")));

        assertThat(output.getPersonId(), is(55));
        assertThat(output.getUpdateTime(), is("2024-04-01T00:00:00Z"));
    }

    @Test
    void shouldRequireAtLeastOneField() {
        RunContext runContext = runContextFactory.of();

        Update task = Update.builder()
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(baseUrl()))
            .personId(Property.ofValue(55))
            .build();

        assertThrows(IllegalArgumentException.class, () -> task.run(runContext));
    }
}
