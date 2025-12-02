package io.kestra.plugin.pipedrive.persons;

import com.fasterxml.jackson.core.type.TypeReference;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.pipedrive.AbstractPipedriveTask;
import io.kestra.plugin.pipedrive.client.PipedriveClient;
import io.kestra.plugin.pipedrive.models.Person;
import io.kestra.plugin.pipedrive.models.PipedriveResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(
    title = "Create a new person in Pipedrive CRM",
    description = "Creates a new person (contact) in your Pipedrive account with the specified details."
)
@Plugin(
    examples = {
        @Example(
            title = "Create a person with basic information",
            full = true,
            code = """
                id: pipedrive_create_person
                namespace: company.team

                tasks:
                  - id: create_person
                    type: io.kestra.plugin.pipedrive.persons.Create
                    apiToken: "{{ secret('PIPEDRIVE_API_TOKEN') }}"
                    name: "John Doe"
                    emails:
                      - value: "john.doe@example.com"
                        primary: true
                    phones:
                      - value: "+1234567890"
                        primary: true
                """
        ),
        @Example(
            title = "Create a person linked to an organization",
            full = true,
            code = """
                id: pipedrive_create_person_with_org
                namespace: company.team

                tasks:
                  - id: create_person
                    type: io.kestra.plugin.pipedrive.persons.Create
                    apiToken: "{{ secret('PIPEDRIVE_API_TOKEN') }}"
                    name: "Jane Smith"
                    orgId: 123
                    emails:
                      - value: "jane.smith@company.com"
                        primary: true
                        label: "work"
                """
        )
    }
)
public class Create extends AbstractPipedriveTask implements RunnableTask<Create.Output> {

    @Schema(
        title = "Person name",
        description = "Full name of the person"
    )
    @NotNull
    private Property<String> name;

    @Schema(
        title = "Organization ID",
        description = "ID of the organization this person belongs to"
    )
    private Property<Integer> orgId;

    @Schema(
        title = "Owner ID",
        description = "ID of the user who will be marked as the owner of this person"
    )
    private Property<Integer> ownerId;

    @Schema(
        title = "Email addresses",
        description = "List of email addresses for this person"
    )
    private Property<List<Map<String, Object>>> emails;

    @Schema(
        title = "Phone numbers",
        description = "List of phone numbers for this person"
    )
    private Property<List<Map<String, Object>>> phones;

    @Schema(
        title = "Visibility",
        description = "Visibility of the person. Valid values: '1' (owner only), '3' (entire company), or '5' (owner and followers)"
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

        String rName = runContext.render(this.name).as(String.class)
            .orElseThrow(() -> new IllegalArgumentException("Person name is required"));

        Integer rOrgId = orgId == null ? null : runContext.render(orgId).as(Integer.class).orElse(null);
        Integer rOwnerId = ownerId == null ? null : runContext.render(ownerId).as(Integer.class).orElse(null);
        String rVisibleTo = visibleTo == null ? null : runContext.render(visibleTo).as(String.class).orElse(null);
        List<Map<String, Object>> rEmails = emails == null ? null : runContext.render(emails).asList(Map.class);
        List<Map<String, Object>> rPhones = phones == null ? null : runContext.render(phones).asList(Map.class);
        Map<String, Object> rCustomFields = customFields == null ? null : runContext.render(customFields).asMap(String.class, Object.class);

        try (PipedriveClient client = new PipedriveClient(runContext, rApiToken, rApiUrl)) {
            Person.PersonBuilder personBuilder = Person.builder().name(rName);

            if (rOrgId != null) {
                personBuilder.orgId(rOrgId);
            }

            if (rOwnerId != null) {
                personBuilder.ownerId(rOwnerId);
            }

            if (rVisibleTo != null) {
                personBuilder.visibleTo(rVisibleTo);
            }

            if (rEmails != null) {
                List<Person.EmailInfo> emailInfos = new ArrayList<>();
                for (Map<String, Object> email : rEmails) {
                    emailInfos.add(Person.EmailInfo.builder()
                        .value((String) email.get("value"))
                        .primary((Boolean) email.getOrDefault("primary", false))
                        .label((String) email.getOrDefault("label", "work"))
                        .build());
                }
                personBuilder.emails(emailInfos);
            }

            if (rPhones != null) {
                List<Person.PhoneInfo> phoneInfos = new ArrayList<>();
                for (Map<String, Object> phone : rPhones) {
                    phoneInfos.add(Person.PhoneInfo.builder()
                        .value((String) phone.get("value"))
                        .primary((Boolean) phone.getOrDefault("primary", false))
                        .label((String) phone.getOrDefault("label", "work"))
                        .build());
                }
                personBuilder.phones(phoneInfos);
            }

            if (rCustomFields != null) {
                personBuilder.customFields(rCustomFields);
            }

            Person person = personBuilder.build();

            logger.info("Creating person in Pipedrive: {}", rName);

            PipedriveResponse<Person> response = client.post("/persons", person,
                new TypeReference<>() {
                });

            if (!Boolean.TRUE.equals(response.getSuccess())) {
                throw new IllegalStateException("Failed to create person: " + response.getError());
            }

            Person createdPerson = response.getData();

            logger.info("Successfully created person with ID: {}", createdPerson.getId());

            return Output.builder()
                .personId(createdPerson.getId())
                .addTime(createdPerson.getAddTime())
                .updateTime(createdPerson.getUpdateTime())
                .build();
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(
            title = "Person ID",
            description = "The ID of the created person in Pipedrive"
        )
        private final Integer personId;

        @Schema(
            title = "Creation time",
            description = "Timestamp when the person was created"
        )
        private final String addTime;

        @Schema(
            title = "Last update time",
            description = "Timestamp when the person was last updated"
        )
        private final String updateTime;
    }
}
