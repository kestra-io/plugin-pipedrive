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
                    type: io.kestra.plugin.pipedrive.deals.CreateDeal
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
                    type: io.kestra.plugin.pipedrive.deals.CreateDeal
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
public class CreateDeal extends Task implements RunnableTask<CreateDeal.Output> {

    @Schema(
        title = "Pipedrive API token",
        description = "Your Pipedrive API token for authentication"
    )
    @PluginProperty(dynamic = true)
    private Property<String> apiToken;

    @Schema(
        title = "Deal title",
        description = "Title of the deal"
    )
    @PluginProperty(dynamic = true)
    private Property<String> title;

    @Schema(
        title = "Deal value",
        description = "Value of the deal"
    )
    @PluginProperty(dynamic = true)
    private Property<BigDecimal> value;

    @Schema(
        title = "Currency",
        description = "Currency code (e.g., USD, EUR, GBP)"
    )
    @PluginProperty(dynamic = true)
    private Property<String> currency;

    @Schema(
        title = "Person ID",
        description = "ID of the person this deal is associated with"
    )
    @PluginProperty(dynamic = true)
    private Property<Integer> personId;

    @Schema(
        title = "Organization ID",
        description = "ID of the organization this deal is associated with"
    )
    @PluginProperty(dynamic = true)
    private Property<Integer> orgId;

    @Schema(
        title = "User ID",
        description = "ID of the user who will be marked as the owner of this deal"
    )
    @PluginProperty(dynamic = true)
    private Property<Integer> userId;

    @Schema(
        title = "Stage ID",
        description = "ID of the stage this deal will be placed in a pipeline"
    )
    @PluginProperty(dynamic = true)
    private Property<Integer> stageId;

    @Schema(
        title = "Status",
        description = "Status of the deal. Valid values: 'open', 'won', 'lost', 'deleted'"
    )
    @PluginProperty(dynamic = true)
    private Property<String> status;

    @Schema(
        title = "Expected close date",
        description = "Expected close date in YYYY-MM-DD format"
    )
    @PluginProperty(dynamic = true)
    private Property<String> expectedCloseDate;

    @Schema(
        title = "Probability",
        description = "Deal success probability percentage (0-100)"
    )
    @PluginProperty(dynamic = true)
    private Property<Double> probability;

    @Schema(
        title = "Visibility",
        description = "Visibility of the deal. Valid values: '1' (owner only), '3' (entire company), or '5' (owner and followers)"
    )
    @PluginProperty(dynamic = true)
    private Property<String> visibleTo;

    @Schema(
        title = "Custom fields",
        description = "Map of custom field keys and values"
    )
    @PluginProperty(dynamic = true)
    private Property<Map<String, Object>> customFields;

    @Override
    public Output run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();
        
        String token = runContext.render(this.apiToken).as(String.class)
            .orElseThrow(() -> new IllegalArgumentException("API token is required"));
        
        String dealTitle = runContext.render(this.title).as(String.class)
            .orElseThrow(() -> new IllegalArgumentException("Deal title is required"));

        PipedriveClient client = new PipedriveClient(token, logger);

        try {
            Deal.DealBuilder dealBuilder = Deal.builder()
                .title(dealTitle);

            // Add optional fields
            if (value != null) {
                dealBuilder.value(runContext.render(value).as(BigDecimal.class).orElse(null));
            }

            if (currency != null) {
                dealBuilder.currency(runContext.render(currency).as(String.class).orElse("USD"));
            }

            if (personId != null) {
                dealBuilder.personId(runContext.render(personId).as(Integer.class).orElse(null));
            }

            if (orgId != null) {
                dealBuilder.orgId(runContext.render(orgId).as(Integer.class).orElse(null));
            }

            if (userId != null) {
                dealBuilder.userId(runContext.render(userId).as(Integer.class).orElse(null));
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

            if (visibleTo != null) {
                dealBuilder.visibleTo(runContext.render(visibleTo).as(String.class).orElse(null));
            }

            if (customFields != null) {
                dealBuilder.customFields(runContext.render(customFields).asMap(String.class, Object.class));
            }

            Deal deal = dealBuilder.build();

            logger.info("Creating deal in Pipedrive: {}", dealTitle);
            
            PipedriveResponse<Deal> response = client.post("/deals", deal, 
                new TypeReference<PipedriveResponse<Deal>>() {});

            if (!response.getSuccess()) {
                throw new RuntimeException("Failed to create deal: " + response.getError());
            }

            Deal createdDeal = response.getData();
            
            logger.info("Successfully created deal with ID: {}", createdDeal.getId());

            return Output.builder()
                .dealId(createdDeal.getId())
                .title(createdDeal.getTitle())
                .value(createdDeal.getValue())
                .currency(createdDeal.getCurrency())
                .status(createdDeal.getStatus())
                .build();
        } finally {
            client.close();
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Deal ID", description = "The ID of the created deal in Pipedrive")
        private final Integer dealId;

        @Schema(title = "Deal title", description = "The title of the created deal")
        private final String title;

        @Schema(title = "Deal value", description = "The value of the created deal")
        private final BigDecimal value;

        @Schema(title = "Currency", description = "The currency of the deal")
        private final String currency;

        @Schema(title = "Status", description = "The status of the deal")
        private final String status;
    }
}
