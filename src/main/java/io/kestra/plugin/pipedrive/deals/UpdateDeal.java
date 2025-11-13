package io.kestra.plugin.pipedrive.deals;

import com.fasterxml.jackson.core.type.TypeReference;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.pipedrive.client.PipedriveClient;
import io.kestra.plugin.pipedrive.models.Deal;
import io.kestra.plugin.pipedrive.models.PipedriveResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Update an existing deal in Pipedrive CRM",
    description = "Updates an existing deal in Pipedrive with new information."
)
@Plugin(
    examples = {
        @Example(
            title = "Update deal value and stage",
            full = true,
            code = """
                id: pipedrive_update_deal
                namespace: company.team
                
                tasks:
                  - id: update_deal
                    type: io.kestra.plugin.pipedrive.deals.UpdateDeal
                    apiToken: "{{ secret('PIPEDRIVE_API_TOKEN') }}"
                    dealId: 123
                    value: 75000
                    stageId: 2
                """
        ),
        @Example(
            title = "Mark deal as won",
            full = true,
            code = """
                id: pipedrive_win_deal
                namespace: company.team
                
                tasks:
                  - id: win_deal
                    type: io.kestra.plugin.pipedrive.deals.UpdateDeal
                    apiToken: "{{ secret('PIPEDRIVE_API_TOKEN') }}"
                    dealId: 123
                    status: "won"
                """
        )
    }
)
public class UpdateDeal extends Task implements RunnableTask<UpdateDeal.Output> {

    @Schema(
        title = "Pipedrive API token",
        description = "Your Pipedrive API token for authentication"
    )
    @PluginProperty(dynamic = true)
    private Property<String> apiToken;

    @Schema(
        title = "Deal ID",
        description = "ID of the deal to update"
    )
    @PluginProperty(dynamic = true)
    private Property<Integer> dealId;

    @Schema(
        title = "Deal title",
        description = "New title for the deal"
    )
    @PluginProperty(dynamic = true)
    private Property<String> title;

    @Schema(
        title = "Deal value",
        description = "New value for the deal"
    )
    @PluginProperty(dynamic = true)
    private Property<BigDecimal> value;

    @Schema(
        title = "Stage ID",
        description = "New stage ID for the deal"
    )
    @PluginProperty(dynamic = true)
    private Property<Integer> stageId;

    @Schema(
        title = "Status",
        description = "New status for the deal. Valid values: 'open', 'won', 'lost'"
    )
    @PluginProperty(dynamic = true)
    private Property<String> status;

    @Schema(
        title = "Expected close date",
        description = "New expected close date in YYYY-MM-DD format"
    )
    @PluginProperty(dynamic = true)
    private Property<String> expectedCloseDate;

    @Schema(
        title = "Probability",
        description = "New deal success probability percentage (0-100)"
    )
    @PluginProperty(dynamic = true)
    private Property<Double> probability;

    @Schema(
        title = "Lost reason",
        description = "Reason for losing the deal (if status is 'lost')"
    )
    @PluginProperty(dynamic = true)
    private Property<String> lostReason;

    @Schema(
        title = "Custom fields",
        description = "Map of custom field keys and values to update"
    )
    @PluginProperty(dynamic = true)
    private Property<Map<String, Object>> customFields;

    @Override
    public Output run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();
        
        String token = runContext.render(this.apiToken).as(String.class)
            .orElseThrow(() -> new IllegalArgumentException("API token is required"));
        
        Integer id = runContext.render(this.dealId).as(Integer.class)
            .orElseThrow(() -> new IllegalArgumentException("Deal ID is required"));

        PipedriveClient client = new PipedriveClient(token, logger);

        try {
            Deal.DealBuilder dealBuilder = Deal.builder();

            // Add fields to update
            if (title != null) {
                dealBuilder.title(runContext.render(title).as(String.class).orElse(null));
            }

            if (value != null) {
                dealBuilder.value(runContext.render(value).as(BigDecimal.class).orElse(null));
            }

            if (stageId != null) {
                dealBuilder.stageId(runContext.render(stageId).as(Integer.class).orElse(null));
            }

            if (status != null) {
                dealBuilder.status(runContext.render(status).as(String.class).orElse(null));
            }

            if (expectedCloseDate != null) {
                dealBuilder.expectedCloseDate(runContext.render(expectedCloseDate).as(String.class).orElse(null));
            }

            if (probability != null) {
                dealBuilder.probability(runContext.render(probability).as(Double.class).orElse(null));
            }

            if (lostReason != null) {
                dealBuilder.lostReason(runContext.render(lostReason).as(String.class).orElse(null));
            }

            if (customFields != null) {
                dealBuilder.customFields(runContext.render(customFields).asMap(String.class, Object.class));
            }

            Deal deal = dealBuilder.build();

            logger.info("Updating deal with ID: {}", id);
            
            PipedriveResponse<Deal> response = client.put("/deals/" + id, deal, 
                new TypeReference<PipedriveResponse<Deal>>() {});

            if (!response.getSuccess()) {
                throw new RuntimeException("Failed to update deal: " + response.getError());
            }

            Deal updatedDeal = response.getData();
            
            logger.info("Successfully updated deal: {}", updatedDeal.getTitle());

            return Output.builder()
                .dealId(updatedDeal.getId())
                .title(updatedDeal.getTitle())
                .value(updatedDeal.getValue())
                .status(updatedDeal.getStatus())
                .build();
        } finally {
            client.close();
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Deal ID")
        private final Integer dealId;

        @Schema(title = "Deal title")
        private final String title;

        @Schema(title = "Deal value")
        private final BigDecimal value;

        @Schema(title = "Status")
        private final String status;
    }
}
