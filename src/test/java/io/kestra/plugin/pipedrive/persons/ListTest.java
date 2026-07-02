package io.kestra.plugin.pipedrive.persons;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.common.FetchType;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;

import jakarta.inject.Inject;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@KestraTest
class ListTest {
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
    void shouldListPersonsWithFilters() throws Exception {
        mockWebServer.enqueue(
            new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                    {
                      "success": true,
                      "data": [
                        {"id": 1, "name": "John Doe"},
                        {"id": 2, "name": "Jane Smith"}
                      ],
                      "additional_data": {
                        "next_cursor": "eyJvcmdfaWQiOjF9"
                      }
                    }
                    """)
        );

        RunContext runContext = runContextFactory.of();

        List task = List.builder()
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(baseUrl()))
            .ownerId(Property.ofValue(7))
            .limit(Property.ofValue(50))
            .build();

        List.Output output = task.run(runContext);

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getMethod(), is("GET"));
        assertThat(recordedRequest.getPath(), containsString("/v2/persons?"));
        assertThat(recordedRequest.getPath(), containsString("owner_id=7"));
        assertThat(recordedRequest.getPath(), containsString("limit=50"));
        assertThat(recordedRequest.getHeader("x-api-token"), is("token"));

        assertThat(output.getPersons().size(), is(2));
        assertThat(output.getCount(), is(2));
        assertThat(output.getNextCursor(), is("eyJvcmdfaWQiOjF9"));
    }

    @Test
    void shouldReturnEmptyResultsWithoutError() throws Exception {
        mockWebServer.enqueue(
            new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                    {
                      "success": true,
                      "data": []
                    }
                    """)
        );

        RunContext runContext = runContextFactory.of();

        List task = List.builder()
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(baseUrl()))
            .build();

        List.Output output = task.run(runContext);

        assertThat(output.getPersons(), emptyIterable());
        assertThat(output.getCount(), is(0));
        assertThat(output.getNextCursor(), nullValue());
    }

    @Test
    void shouldRejectFetchOneStrategy() {
        RunContext runContext = runContextFactory.of();

        List task = List.builder()
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(baseUrl()))
            .fetchType(Property.ofValue(FetchType.FETCH_ONE))
            .build();

        assertThrows(IllegalArgumentException.class, () -> task.run(runContext));
    }
}
