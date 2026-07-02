package io.kestra.plugin.pipedrive.deals;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.URI;
import java.util.List;

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
    title = "Get a deal from Pipedrive CRM",
    description = "Retrieves detailed information about a specific deal from Pipedrive by its ID."
)
@Plugin(
    examples = {
        @Example(
            title = "Get a deal by ID",
            full = true,
            code = """
                id: pipedrive_get_deal
                namespace: company.team

                tasks:
                  - id: get_deal
                    type: io.kestra.plugin.pipedrive.deals.Get
                    apiToken: "{{ secret('PIPEDRIVE_API_TOKEN') }}"
                    dealId: 123
                """
        )
    }
)
public class Get extends AbstractPipedriveTask implements RunnableTask<Get.Output> {

    @Schema(
        title = "Deal ID",
        description = "The ID of the deal to retrieve"
    )
    @NotNull
    @PluginProperty(group = "main")
    private Property<Integer> dealId;

    @Schema(
        title = "Fetch strategy",
        description = "How to fetch the deal data (fetch, fetch one, or store)"
    )
    @NotNull
    @Builder.Default
    @PluginProperty(group = "processing")
    private Property<FetchType> fetchType = Property.ofValue(FetchType.FETCH_ONE);

    @Override
    public Output run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();

        var rApiToken = this.renderApiToken(runContext);
        var rApiUrl = this.renderApiUrl(runContext);

        var rDealId = runContext.render(this.dealId).as(Integer.class)
            .orElseThrow(() -> new IllegalArgumentException("Deal ID is required"));

        try (PipedriveClient client = new PipedriveClient(runContext, rApiToken, rApiUrl)) {
            logger.info("Fetching deal with ID: {}", rDealId);

            PipedriveResponse<Deal> response = client.get(
                "/deals/" + rDealId,
                new TypeReference<>() {
                }
            );

            if (!Boolean.TRUE.equals(response.getSuccess())) {
                throw new IllegalStateException("Failed to get deal: " + response.getError());
            }

            var deal = response.getData();

            logger.info("Successfully retrieved deal: {}", deal.getTitle());

            var rFetchType = runContext.render(this.fetchType).as(FetchType.class).orElse(FetchType.FETCH_ONE);

            return switch (rFetchType) {
                case STORE -> {
                    File tempFile = runContext.workingDir().createTempFile(".ion").toFile();
                    try (BufferedWriter output = new BufferedWriter(new FileWriter(tempFile), FileSerde.BUFFER_SIZE)) {
                        Long count = FileSerde.writeAll(output, Flux.just(deal)).block();
                        URI uri = runContext.storage().putFile(tempFile);
                        yield Output.builder()
                            .uri(uri)
                            .count(count != null ? count.intValue() : 0)
                            .build();
                    }
                }
                case FETCH -> Output.builder()
                    .deals(List.of(deal))
                    .count(1)
                    .build();
                case FETCH_ONE -> Output.builder()
                    .deal(deal)
                    .count(1)
                    .build();
                default -> Output.builder()
                    .deal(deal)
                    .count(1)
                    .build();
            };
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Deal", description = "The retrieved deal")
        private final Deal deal;

        @Schema(title = "Deals", description = "List of retrieved deals when fetchType is FETCH")
        private final List<Deal> deals;

        @Schema(title = "URI", description = "Stored deal location when fetchType is STORE")
        private final URI uri;

        @Schema(title = "Count", description = "Number of deals retrieved or stored")
        private final Integer count;
    }
}
