package io.kestra.plugin.pipedrive.deals;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

@KestraTest
class GetTest {
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
    @SuppressWarnings("unchecked")
    void shouldStoreFetchedDeal() throws Exception {
        mockWebServer.enqueue(
            new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                    {
                      "success": true,
                      "data": {
                        "id": 321,
                        "title": "Enterprise Software License",
                        "status": "open"
                      }
                    }
                    """)
        );

        RunContext runContext = runContextFactory.of();

        Get task = Get.builder()
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(baseUrl()))
            .dealId(Property.ofValue(321))
            .fetchType(Property.ofValue(FetchType.STORE))
            .build();

        Get.Output output = task.run(runContext);

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getMethod(), is("GET"));
        assertThat(recordedRequest.getPath(), is("/v2/deals/321"));
        assertThat(recordedRequest.getPath(), not(containsString("api_token")));
        assertThat(recordedRequest.getHeader("x-api-token"), is("token"));

        URI uri = output.getUri();
        assertThat(uri, notNullValue());

        List<Map<String, Object>> stored = new ArrayList<>();
        try (
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                    storageInterface.get(TenantService.MAIN_TENANT, null, uri)
                )
            )
        ) {
            FileSerde.reader(reader, value -> stored.add((Map<String, Object>) value));
        }

        assertThat(stored.getFirst().get("id"), is(321));
        assertThat(output.getCount(), is(1));
    }

    @Test
    void shouldFetchOneDeal() throws Exception {
        mockWebServer.enqueue(
            new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                    {
                      "success": true,
                      "data": {
                        "id": 321,
                        "title": "Enterprise Software License",
                        "status": "open"
                      }
                    }
                    """)
        );

        RunContext runContext = runContextFactory.of();

        Get task = Get.builder()
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(baseUrl()))
            .dealId(Property.ofValue(321))
            .build();

        Get.Output output = task.run(runContext);

        assertThat(output.getDeal().getId(), is(321));
        assertThat(output.getDeal().getTitle(), is("Enterprise Software License"));
        assertThat(output.getCount(), is(1));
    }

    @Test
    void shouldThrowWhenDealNotFound() {
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

        Get task = Get.builder()
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(baseUrl()))
            .dealId(Property.ofValue(999))
            .build();

        IllegalStateException exception = org.junit.jupiter.api.Assertions.assertThrows(
            IllegalStateException.class, () -> task.run(runContext)
        );
        assertThat(exception.getMessage(), containsString("Deal not found"));
    }
}
