package io.kestra.plugin.pipedrive.deals;

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
import io.kestra.plugin.pipedrive.models.Deal;
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
    title = "List and filter deals in Pipedrive CRM",
    description = "Searches deals in Pipedrive using field filters such as pipeline, stage, status, or owner."
)
@Plugin(
    examples = {
        @Example(
            title = "List open deals in a pipeline",
            full = true,
            code = """
                id: pipedrive_list_deals
                namespace: company.team

                tasks:
                  - id: list_deals
                    type: io.kestra.plugin.pipedrive.deals.List
                    apiToken: "{{ secret('PIPEDRIVE_API_TOKEN') }}"
                    pipelineId: 1
                    status: OPEN
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
        title = "Person ID",
        description = "Only return deals associated with this person"
    )
    @PluginProperty(group = "processing")
    private Property<Integer> personId;

    @Schema(
        title = "Organization ID",
        description = "Only return deals associated with this organization"
    )
    @PluginProperty(group = "processing")
    private Property<Integer> orgId;

    @Schema(
        title = "Pipeline ID",
        description = "Only return deals in this pipeline"
    )
    @PluginProperty(group = "processing")
    private Property<Integer> pipelineId;

    @Schema(
        title = "Stage ID",
        description = "Only return deals in this pipeline stage"
    )
    @PluginProperty(group = "processing")
    private Property<Integer> stageId;

    @Schema(
        title = "Status",
        description = "Only return deals with this status. Valid values: OPEN, WON, LOST, DELETED"
    )
    @PluginProperty(group = "processing")
    private Property<DealStatus> status;

    @Schema(
        title = "Owner ID",
        description = "Only return deals owned by this user"
    )
    @PluginProperty(group = "processing")
    private Property<Integer> ownerId;

    @Schema(
        title = "Limit",
        description = "Maximum number of deals to return in a single page"
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
        description = "How to return the fetched deals. Valid values: 'FETCH' (as a list of objects) or 'STORE' (to internal storage). Defaults to 'FETCH'."
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
        if (personId != null) {
            runContext.render(personId).as(Integer.class).ifPresent(v -> queryParams.put("person_id", String.valueOf(v)));
        }
        if (orgId != null) {
            runContext.render(orgId).as(Integer.class).ifPresent(v -> queryParams.put("org_id", String.valueOf(v)));
        }
        if (pipelineId != null) {
            runContext.render(pipelineId).as(Integer.class).ifPresent(v -> queryParams.put("pipeline_id", String.valueOf(v)));
        }
        if (stageId != null) {
            runContext.render(stageId).as(Integer.class).ifPresent(v -> queryParams.put("stage_id", String.valueOf(v)));
        }
        if (status != null) {
            runContext.render(status).as(DealStatus.class).ifPresent(v -> queryParams.put("status", v.toString()));
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
            logger.info("Listing Pipedrive deals with filters: {}", queryParams);

            PipedriveResponse<java.util.List<Deal>> response = client.get(
                "/deals", queryParams,
                new TypeReference<>() {
                }
            );

            if (!Boolean.TRUE.equals(response.getSuccess())) {
                throw new IllegalStateException("Failed to list deals: " + response.getError());
            }

            var deals = response.getData() != null ? response.getData() : java.util.List.<Deal> of();
            var rNextCursor = response.getAdditionalData() != null ? response.getAdditionalData().getNextCursor() : null;

            logger.info("Successfully listed {} deal(s)", deals.size());

            return switch (rFetchType) {
                case STORE -> {
                    File tempFile = runContext.workingDir().createTempFile(".ion").toFile();
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile), FileSerde.BUFFER_SIZE)) {
                        Long count = FileSerde.writeAll(writer, Flux.fromIterable(deals)).block();
                        URI uri = runContext.storage().putFile(tempFile);
                        yield Output.builder()
                            .uri(uri)
                            .count(count != null ? count.intValue() : 0)
                            .nextCursor(rNextCursor)
                            .build();
                    }
                }
                case FETCH -> Output.builder()
                    .deals(deals)
                    .count(deals.size())
                    .nextCursor(rNextCursor)
                    .build();
                default -> throw new IllegalStateException("Unexpected fetch type: " + rFetchType);
            };
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Deals", description = "List of deals matching the filters, when fetchType is FETCH")
        private final java.util.List<Deal> deals;

        @Schema(title = "URI", description = "Stored deals location when fetchType is STORE")
        private final URI uri;

        @Schema(title = "Count", description = "Number of deals retrieved or stored")
        private final Integer count;

        @Schema(title = "Next cursor", description = "Pagination cursor to pass as `cursor` on a subsequent run to fetch the next page, or null if there are no more results")
        private final String nextCursor;
    }
}
