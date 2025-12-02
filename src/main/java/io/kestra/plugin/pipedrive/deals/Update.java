package io.kestra.plugin.pipedrive.deals;

import com.fasterxml.jackson.core.type.TypeReference;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.pipedrive.AbstractPipedriveTask;
import io.kestra.plugin.pipedrive.client.PipedriveClient;
import io.kestra.plugin.pipedrive.models.Deal;
import io.kestra.plugin.pipedrive.models.PipedriveResponse;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
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
                    type: io.kestra.plugin.pipedrive.deals.Update
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
                    type: io.kestra.plugin.pipedrive.deals.Update
                    apiToken: "{{ secret('PIPEDRIVE_API_TOKEN') }}"
                    dealId: 123
                    status: "won"
                """
        )
    }
)
public class Update extends AbstractPipedriveTask implements RunnableTask<Update.Output> {

    @Schema(
        title = "Deal ID",
        description = "ID of the deal to update"
    )
    @NotNull
    private Property<Integer> dealId;

    @Schema(
        title = "Deal title",
        description = "New title for the deal"
    )
    private Property<String> title;

    @Schema(
        title = "Deal value",
        description = "New value for the deal"
    )
    private Property<BigDecimal> value;

    @Schema(
        title = "Stage ID",
        description = "New stage ID for the deal"
    )
    private Property<Integer> stageId;

    @Schema(
        title = "Status",
        description = "New status for the deal. Valid values: 'open', 'won', 'lost'"
    )
    private Property<String> status;

    @Schema(
        title = "Expected close date",
        description = "New expected close date in YYYY-MM-DD format"
    )
    private Property<String> expectedCloseDate;

    @Schema(
        title = "Probability",
        description = "New deal success probability percentage (0-100)"
    )
    private Property<Double> probability;

    @Schema(
        title = "Lost reason",
        description = "Reason for losing the deal (if status is 'lost')"
    )
    private Property<String> lostReason;

    @Schema(
        title = "Custom fields",
        description = "Map of custom field keys and values to update"
    )
    private Property<Map<String, Object>> customFields;

    @Override
    public Output run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();

        String rApiToken = this.renderApiToken(runContext);
        String rApiUrl = this.renderApiUrl(runContext);

        Integer rDealId = runContext.render(this.dealId).as(Integer.class)
            .orElseThrow(() -> new IllegalArgumentException("Deal ID is required"));

        if (title == null && value == null && stageId == null && status == null
            && expectedCloseDate == null && probability == null && lostReason == null && customFields == null) {
            throw new IllegalArgumentException("At least one field must be provided to update the deal");
        }

        String rTitle = title == null ? null : runContext.render(title).as(String.class).orElse(null);
        BigDecimal rValue = value == null ? null : runContext.render(value).as(BigDecimal.class).orElse(null);
        Integer rStageId = stageId == null ? null : runContext.render(stageId).as(Integer.class).orElse(null);
        String rStatus = status == null ? null : runContext.render(status).as(String.class).orElse(null);
        String rExpectedCloseDate = expectedCloseDate == null ? null : runContext.render(expectedCloseDate).as(String.class).orElse(null);
        Double rProbability = probability == null ? null : runContext.render(probability).as(Double.class).orElse(null);
        String rLostReason = lostReason == null ? null : runContext.render(lostReason).as(String.class).orElse(null);
        Map<String, Object> rCustomFields = customFields == null ? null : runContext.render(customFields).asMap(String.class, Object.class);

        try (PipedriveClient client = new PipedriveClient(runContext, rApiToken, rApiUrl)) {
            Deal.DealBuilder dealBuilder = Deal.builder();

            if (rTitle != null) {
                dealBuilder.title(rTitle);
            }

            if (rValue != null) {
                dealBuilder.value(rValue);
            }

            if (rStageId != null) {
                dealBuilder.stageId(rStageId);
            }

            if (rStatus != null) {
                dealBuilder.status(rStatus);
            }

            if (rExpectedCloseDate != null) {
                dealBuilder.expectedCloseDate(rExpectedCloseDate);
            }

            if (rProbability != null) {
                dealBuilder.probability(rProbability);
            }

            if (rLostReason != null) {
                dealBuilder.lostReason(rLostReason);
            }

            if (rCustomFields != null) {
                dealBuilder.customFields(rCustomFields);
            }

            Deal deal = dealBuilder.build();

            logger.info("Updating Pipedrive deal with ID: {}", rDealId);

            PipedriveResponse<Deal> response = client.put("/deals/" + rDealId, deal,
                new TypeReference<>() {
                });

            if (!Boolean.TRUE.equals(response.getSuccess())) {
                throw new IllegalStateException("Failed to update deal: " + response.getError());
            }

            Deal updatedDeal = response.getData();

            logger.info("Successfully updated deal: {}", updatedDeal.getId());

            return Output.builder()
                .dealId(updatedDeal.getId())
                .updateTime(updatedDeal.getUpdateTime())
                .build();
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Deal ID")
        private final Integer dealId;

        @Schema(title = "Last update time", description = "Timestamp when the deal was last updated")
        private final String updateTime;
    }
}
