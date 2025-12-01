package io.kestra.plugin.pipedrive.persons;

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

import java.util.List;
import java.util.Map;

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
    void shouldCreatePerson() throws Exception {
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody("""
                {
                  "success": true,
                  "data": {
                    "id": 55,
                    "name": "Jane Doe",
                    "first_name": "Jane",
                    "last_name": "Doe",
                    "add_time": "2024-03-01T00:00:00Z",
                    "update_time": "2024-03-01T00:00:00Z"
                  }
                }
                """));

        RunContext runContext = runContextFactory.of();

        Create task = Create.builder()
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(baseUrl()))
            .name(Property.ofValue("Jane Doe"))
            .orgId(Property.ofValue(123))
            .visibleTo(Property.ofValue("3"))
            .emails(Property.ofValue(List.of(
                Map.of("value", "jane.doe@example.com", "primary", true, "label", "work")
            )))
            .phones(Property.ofValue(List.of(
                Map.of("value", "+1234567890", "primary", true, "label", "mobile")
            )))
            .build();

        Create.Output output = task.run(runContext);

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        String body = recordedRequest.getBody().readUtf8();

        assertThat(recordedRequest.getMethod(), is("POST"));
        assertThat(recordedRequest.getPath(), containsString("/persons?api_token=token"));
        assertThat(body, allOf(
            containsString("Jane Doe"),
            containsString("jane.doe@example.com"),
            containsString("+1234567890"),
            containsString("visible_to")
        ));

        assertThat(output.getPersonId(), is(55));
        assertThat(output.getAddTime(), is("2024-03-01T00:00:00Z"));
    }
}
