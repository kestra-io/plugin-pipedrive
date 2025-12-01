package io.kestra.plugin.pipedrive.persons;

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
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
    void shouldStoreFetchedPerson() throws Exception {
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody("""
                {
                  "success": true,
                  "data": {
                    "id": 12,
                    "name": "Jane Doe",
                    "first_name": "Jane",
                    "last_name": "Doe"
                  }
                }
                """));

        RunContext runContext = runContextFactory.of();

        Get task = Get.builder()
            .apiToken(Property.ofValue("token"))
            .apiUrl(Property.ofValue(baseUrl()))
            .personId(Property.ofValue(12))
            .fetchType(Property.ofValue(FetchType.STORE))
            .build();

        Get.Output output = task.run(runContext);

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getMethod(), is("GET"));
        assertThat(recordedRequest.getPath(), containsString("/persons/12?api_token=token"));

        URI uri = output.getUri();
        assertThat(uri, notNullValue());

        List<Map<String, Object>> stored = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
            storageInterface.get(TenantService.MAIN_TENANT, null, uri)))) {
            FileSerde.reader(reader, value -> stored.add((Map<String, Object>) value));
        }

        assertThat(stored.getFirst().get("id"), is(12));
        assertThat(output.getCount(), is(1));
    }
}
