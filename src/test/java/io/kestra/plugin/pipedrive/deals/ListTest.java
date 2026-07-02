package io.kestra.plugin.pipedrive.deals;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.common.FetchType;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.serializers.FileSerde;
import io.kestra.core.storages.StorageInterface;
import io.kestra.core.tenant.TenantService;

import jakarta.inject.Inject;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@KestraTest
class ListTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Inject
    private StorageInterface storageInterface;

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
    void shouldListDealsWithFilters() throws Exception {
        mockWebServer.enqueue(
            new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                    {
                      "success": true,
                      "data": [
                        {"id": 1, "title": "Deal 1", "status": "open"},
                        {"id": 2, "title": "Deal 2", "status": "open"}
                      ],
                      "additional_data": {
                        "next_cursor": "eyJmaWx0ZXJfaWQiOjF9"
                      }
                    }
                    """)
        );

        RunContext runContext = runContextFactory.of();

        List task = List.builder()
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(baseUrl()))
            .pipelineId(Property.ofValue(1))
            .status(Property.ofValue(DealStatus.OPEN))
            .limit(Property.ofValue(50))
            .build();

        List.Output output = task.run(runContext);

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getMethod(), is("GET"));
        assertThat(recordedRequest.getPath(), containsString("/v2/deals?"));
        assertThat(recordedRequest.getPath(), containsString("pipeline_id=1"));
        assertThat(recordedRequest.getPath(), containsString("status=open"));
        assertThat(recordedRequest.getPath(), containsString("limit=50"));
        assertThat(recordedRequest.getHeader("x-api-token"), is("token"));

        assertThat(output.getDeals().size(), is(2));
        assertThat(output.getCount(), is(2));
        assertThat(output.getNextCursor(), is("eyJmaWx0ZXJfaWQiOjF9"));
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

        assertThat(output.getDeals(), emptyIterable());
        assertThat(output.getCount(), is(0));
        assertThat(output.getNextCursor(), nullValue());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldStoreListedDeals() throws Exception {
        mockWebServer.enqueue(
            new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                    {
                      "success": true,
                      "data": [
                        {"id": 1, "title": "Deal 1"}
                      ]
                    }
                    """)
        );

        RunContext runContext = runContextFactory.of();

        List task = List.builder()
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(baseUrl()))
            .fetchType(Property.ofValue(FetchType.STORE))
            .build();

        List.Output output = task.run(runContext);

        URI uri = output.getUri();
        assertThat(uri, notNullValue());

        java.util.List<Map<String, Object>> stored = new ArrayList<>();
        try (
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                    storageInterface.get(TenantService.MAIN_TENANT, null, uri)
                )
            )
        ) {
            FileSerde.reader(reader, value -> stored.add((Map<String, Object>) value));
        }

        assertThat(stored.getFirst().get("id"), is(1));
        assertThat(output.getCount(), is(1));
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
