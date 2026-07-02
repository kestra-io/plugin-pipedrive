package io.kestra.plugin.pipedrive.persons;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.URI;
import java.util.LinkedHashMap;

import org.slf4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.common.FetchType;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.FileSerde;
import io.kestra.plugin.pipedrive.AbstractPipedriveTask;
import io.kestra.plugin.pipedrive.client.PipedriveClient;
import io.kestra.plugin.pipedrive.models.Person;
import io.kestra.plugin.pipedrive.models.PipedriveResponse;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import reactor.core.publisher.Flux;

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(
    title = "List and filter persons in Pipedrive CRM",
    description = "Searches persons in Pipedrive using field filters such as organization or owner."
)
@Plugin(
    examples = {
        @Example(
            title = "List persons owned by a user",
            full = true,
            code = """
                id: pipedrive_list_persons
                namespace: company.team

                tasks:
                  - id: list_persons
                    type: io.kestra.plugin.pipedrive.persons.List
                    apiToken: "{{ secret('PIPEDRIVE_API_TOKEN') }}"
                    ownerId: 7
                    limit: 100
                """
        )
    }
)
public class List extends AbstractPipedriveTask implements RunnableTask<List.Output> {

    @Schema(
        title = "Filter ID",
        description = "ID of a saved Pipedrive filter to apply"
    )
    @PluginProperty(group = "processing")
    private Property<Integer> filterId;

    @Schema(
        title = "Organization ID",
        description = "Only return persons associated with this organization"
    )
    @PluginProperty(group = "processing")
    private Property<Integer> orgId;

    @Schema(
        title = "Owner ID",
        description = "Only return persons owned by this user"
    )
    @PluginProperty(group = "processing")
    private Property<Integer> ownerId;

    @Schema(
        title = "Limit",
        description = "Maximum number of persons to return in a single page"
    )
    @PluginProperty(group = "processing")
    private Property<Integer> limit;

    @Schema(
        title = "Cursor",
        description = "Pagination cursor from a previous run's `nextCursor` output, used to fetch the next page of results"
    )
    @PluginProperty(group = "processing")
    private Property<String> cursor;

    @Schema(
        title = "Fetch strategy",
        description = "How to return the fetched persons. Valid values: 'FETCH' (as a list of objects) or 'STORE' (to internal storage). Defaults to 'FETCH'."
    )
    @NotNull
    @Builder.Default
    @PluginProperty(group = "processing")
    private Property<FetchType> fetchType = Property.ofValue(FetchType.FETCH);

    @Override
    public Output run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();

        var rApiToken = this.renderApiToken(runContext);
        var rApiUrl = this.renderApiUrl(runContext);

        var rFetchType = runContext.render(this.fetchType).as(FetchType.class).orElse(FetchType.FETCH);
        if (rFetchType != FetchType.FETCH && rFetchType != FetchType.STORE) {
            throw new IllegalArgumentException("fetchType must be either FETCH or STORE for a list operation");
        }

        var queryParams = new LinkedHashMap<String, String>();
        if (filterId != null) {
            runContext.render(filterId).as(Integer.class).ifPresent(v -> queryParams.put("filter_id", String.valueOf(v)));
        }
        if (orgId != null) {
            runContext.render(orgId).as(Integer.class).ifPresent(v -> queryParams.put("org_id", String.valueOf(v)));
        }
        if (ownerId != null) {
            runContext.render(ownerId).as(Integer.class).ifPresent(v -> queryParams.put("owner_id", String.valueOf(v)));
        }
        if (limit != null) {
            runContext.render(limit).as(Integer.class).ifPresent(v -> queryParams.put("limit", String.valueOf(v)));
        }
        if (cursor != null) {
            runContext.render(cursor).as(String.class).ifPresent(v -> queryParams.put("cursor", v));
        }

        try (PipedriveClient client = new PipedriveClient(runContext, rApiToken, rApiUrl)) {
            logger.info("Listing Pipedrive persons with filters: {}", queryParams);

            PipedriveResponse<java.util.List<Person>> response = client.get(
                "/persons", queryParams,
                new TypeReference<>() {
                }
            );

            if (!Boolean.TRUE.equals(response.getSuccess())) {
                throw new IllegalStateException("Failed to list persons: " + response.getError());
            }

            var persons = response.getData() != null ? response.getData() : java.util.List.<Person> of();
            var rNextCursor = response.getAdditionalData() != null ? response.getAdditionalData().getNextCursor() : null;

            logger.info("Successfully listed {} person(s)", persons.size());

            return switch (rFetchType) {
                case STORE -> {
                    File tempFile = runContext.workingDir().createTempFile(".ion").toFile();
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile), FileSerde.BUFFER_SIZE)) {
                        Long count = FileSerde.writeAll(writer, Flux.fromIterable(persons)).block();
                        URI uri = runContext.storage().putFile(tempFile);
                        yield Output.builder()
                            .uri(uri)
                            .count(count != null ? count.intValue() : 0)
                            .nextCursor(rNextCursor)
                            .build();
                    }
                }
                case FETCH -> Output.builder()
                    .persons(persons)
                    .count(persons.size())
                    .nextCursor(rNextCursor)
                    .build();
                default -> throw new IllegalStateException("Unexpected fetch type: " + rFetchType);
            };
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Persons", description = "List of persons matching the filters, when fetchType is FETCH")
        private final java.util.List<Person> persons;

        @Schema(title = "URI", description = "Stored persons location when fetchType is STORE")
        private final URI uri;

        @Schema(title = "Count", description = "Number of persons retrieved or stored")
        private final Integer count;

        @Schema(title = "Next cursor", description = "Pagination cursor to pass as `cursor` on a subsequent run to fetch the next page, or null if there are no more results")
        private final String nextCursor;
    }
}
