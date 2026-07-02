package io.kestra.plugin.pipedrive.deals;

import java.util.Map;

import org.slf4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.pipedrive.AbstractPipedriveTask;
import io.kestra.plugin.pipedrive.client.PipedriveClient;
import io.kestra.plugin.pipedrive.models.PipedriveResponse;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import io.kestra.core.models.annotations.PluginProperty;

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(
    title = "Delete a deal from Pipedrive CRM",
    description = """
        Deletes a deal in Pipedrive. Pipedrive soft-deletes the record: it is moved to the trash and only \
        purged permanently after 30 days, during which it can still be restored from the Pipedrive UI.

        This task is a cleanup tool, not part of the normal deal lifecycle. To close a deal, use `Update` and \
        set `status` to `won` or `lost` instead of deleting it."""
)
@Plugin(
    examples = {
        @Example(
            title = "Delete a deal by ID",
            full = true,
            code = """
                id: pipedrive_delete_deal
                namespace: company.team

                tasks:
                  - id: delete_deal
                    type: io.kestra.plugin.pipedrive.deals.Delete
                    apiToken: "{{ secret('PIPEDRIVE_API_TOKEN') }}"
                    dealId: 123
                """
        )
    }
)
public class Delete extends AbstractPipedriveTask implements RunnableTask<Delete.Output> {

    @Schema(
        title = "Deal ID",
        description = "The ID of the deal to delete"
    )
    @NotNull
    @PluginProperty(group = "main")
    private Property<Integer> dealId;

    @Override
    public Output run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();

        var rApiToken = this.renderApiToken(runContext);
        var rApiUrl = this.renderApiUrl(runContext);

        var rDealId = runContext.render(this.dealId).as(Integer.class)
            .orElseThrow(() -> new IllegalArgumentException("Deal ID is required"));

        try (PipedriveClient client = new PipedriveClient(runContext, rApiToken, rApiUrl)) {
            logger.info("Deleting Pipedrive deal with ID: {}", rDealId);

            PipedriveResponse<Map<String, Object>> response = client.delete(
                "/deals/" + rDealId,
                new TypeReference<>() {
                }
            );

            if (!Boolean.TRUE.equals(response.getSuccess())) {
                throw new IllegalStateException("Failed to delete deal with ID " + rDealId + ": " + response.getError());
            }

            logger.info("Successfully deleted deal with ID: {}", rDealId);

            return Output.builder()
                .id(rDealId)
                // always true here: a failed deletion throws above instead of returning
                .deleted(true)
                .build();
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Deal ID", description = "The ID of the deleted deal")
        private final Integer id;

        @Schema(title = "Deleted", description = "Whether the deal was successfully deleted")
        private final Boolean deleted;
    }
}
