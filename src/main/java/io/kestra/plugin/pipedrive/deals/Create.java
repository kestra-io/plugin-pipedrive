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
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
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
    title = "Create a new deal in Pipedrive CRM",
    description = "Creates a new deal (opportunity) in your Pipedrive account with the specified details."
)
@Plugin(
    examples = {
        @Example(
            title = "Create a deal with basic information",
            full = true,
            code = """
                id: pipedrive_create_deal
                namespace: company.team

                tasks:
                  - id: create_deal
                    type: io.kestra.plugin.pipedrive.deals.Create
                    apiToken: "{{ secret('PIPEDRIVE_API_TOKEN') }}"
                    title: "Enterprise Software License"
                    value: 50000
                    currency: "USD"
                """
        ),
        @Example(
            title = "Create a deal linked to a person",
            full = true,
            code = """
                id: pipedrive_create_deal_with_person
                namespace: company.team

                tasks:
                  - id: create_deal
                    type: io.kestra.plugin.pipedrive.deals.Create
                    apiToken: "{{ secret('PIPEDRIVE_API_TOKEN') }}"
                    title: "Cloud Services Contract"
                    value: 100000
                    currency: "USD"
                    personId: 123
                    stageId: 1
                    expectedCloseDate: "2024-12-31"
                """
        )
    }
)
public class Create extends AbstractPipedriveTask implements RunnableTask<Create.Output> {

    @Schema(
        title = "Deal title"
    )
    @NotNull
    private Property<String> title;

    @Schema(
        title = "Deal value"
    )
    private Property<BigDecimal> value;

    @Schema(
        title = "Currency",
        description = "Currency code (e.g., USD, EUR, GBP)"
    )
    private Property<String> currency;

    @Schema(
        title = "Person ID",
        description = "ID of the person this deal is associated with"
    )
    private Property<Integer> personId;

    @Schema(
        title = "Organization ID",
        description = "ID of the organization this deal is associated with"
    )
    private Property<Integer> orgId;

    @Schema(
        title = "User ID",
        description = "ID of the user who will be marked as the owner of this deal"
    )
    private Property<Integer> userId;

    @Schema(
        title = "Stage ID",
        description = "ID of the stage this deal will be placed in a pipeline"
    )
    private Property<Integer> stageId;

    @Schema(
        title = "Status",
        description = "Status of the deal. Valid values: 'open', 'won', 'lost', 'deleted'"
    )
    private Property<String> status;

    @Schema(
        title = "Expected close date",
        description = "Expected close date in YYYY-MM-DD format"
    )
    private Property<String> expectedCloseDate;

    @Schema(
        title = "Probability",
        description = "Deal success probability percentage (0-100)"
    )
    private Property<Double> probability;

    @Schema(
        title = "Visibility",
        description = "Visibility of the deal. Valid values: '1' (owner only), '3' (entire company), or '5' (owner and followers)"
    )
    private Property<String> visibleTo;

    @Schema(
        title = "Custom fields",
        description = "Map of custom field keys and values"
    )
    private Property<Map<String, Object>> customFields;

    @Override
    public Output run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();

        String rApiToken = this.renderApiToken(runContext);
        String rApiUrl = this.renderApiUrl(runContext);

        String rDealTitle = runContext.render(this.title).as(String.class)
            .orElseThrow(() -> new IllegalArgumentException("Deal title is required"));

        BigDecimal rValue = value == null ? null : runContext.render(value).as(BigDecimal.class).orElse(null);
        String rCurrency = currency == null ? null : runContext.render(currency).as(String.class).orElse("USD");
        Integer rPersonId = personId == null ? null : runContext.render(personId).as(Integer.class).orElse(null);
        Integer rOrgId = orgId == null ? null : runContext.render(orgId).as(Integer.class).orElse(null);
        Integer rUserId = userId == null ? null : runContext.render(userId).as(Integer.class).orElse(null);
        Integer rStageId = stageId == null ? null : runContext.render(stageId).as(Integer.class).orElse(null);
        String rStatus = status == null ? null : runContext.render(status).as(String.class).orElse(null);
        String rExpectedCloseDate = expectedCloseDate == null ? null : runContext.render(expectedCloseDate).as(String.class).orElse(null);
        Double rProbability = probability == null ? null : runContext.render(probability).as(Double.class).orElse(null);
        String rVisibleTo = visibleTo == null ? null : runContext.render(visibleTo).as(String.class).orElse(null);
        Map<String, Object> rCustomFields = customFields == null ? null : runContext.render(customFields).asMap(String.class, Object.class);

        try (PipedriveClient client = new PipedriveClient(runContext, rApiToken, rApiUrl)) {
            Deal.DealBuilder dealBuilder = Deal.builder()
                .title(rDealTitle);

            if (rValue != null) {
                dealBuilder.value(rValue);
            }

            if (rCurrency != null) {
                dealBuilder.currency(rCurrency);
            }

            if (rPersonId != null) {
                dealBuilder.personId(rPersonId);
            }

            if (rOrgId != null) {
                dealBuilder.orgId(rOrgId);
            }

            if (rUserId != null) {
                dealBuilder.userId(rUserId);
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

            if (rVisibleTo != null) {
                dealBuilder.visibleTo(rVisibleTo);
            }

            if (rCustomFields != null) {
                dealBuilder.customFields(rCustomFields);
            }

            Deal deal = dealBuilder.build();

            logger.info("Creating Pipedrive deal '{}'", rDealTitle);

            PipedriveResponse<Deal> response = client.post("/deals", deal,
                new TypeReference<>() {
                });

            if (!Boolean.TRUE.equals(response.getSuccess())) {
                throw new IllegalStateException("Failed to create deal: " + response.getError());
            }

            Deal createdDeal = response.getData();

            logger.info("Successfully created deal with ID: {}", createdDeal.getId());

            return Output.builder()
                .dealId(createdDeal.getId())
                .addTime(createdDeal.getAddTime())
                .updateTime(createdDeal.getUpdateTime())
                .build();
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Deal ID", description = "The ID of the created deal in Pipedrive")
        private final Integer dealId;

        @Schema(title = "Creation time", description = "Timestamp when the deal was created")
        private final String addTime;

        @Schema(title = "Last update time", description = "Timestamp when the deal was last updated")
        private final String updateTime;
    }
}
