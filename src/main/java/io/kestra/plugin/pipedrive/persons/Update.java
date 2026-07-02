package io.kestra.plugin.pipedrive.persons;

import java.util.ArrayList;
import java.util.List;
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
import io.kestra.core.models.annotations.PluginProperty;

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(
    title = "Update an existing person in Pipedrive CRM",
    description = "Updates an existing person in Pipedrive with new information."
)
@Plugin(
    examples = {
        @Example(
            title = "Update a person's organization and email",
            full = true,
            code = """
                id: pipedrive_update_person
                namespace: company.team

                tasks:
                  - id: update_person
                    type: io.kestra.plugin.pipedrive.persons.Update
                    apiToken: "{{ secret('PIPEDRIVE_API_TOKEN') }}"
                    personId: 55
                    orgId: 42
                    emails:
                      - value: "jane.doe@newcompany.com"
                        primary: true
                """
        )
    }
)
public class Update extends AbstractPipedriveTask implements RunnableTask<Update.Output> {

    @Schema(
        title = "Person ID"
    )
    @NotNull
    @PluginProperty(group = "main")
    private Property<Integer> personId;

    @Schema(
        title = "Person name",
        description = "New full name for the person"
    )
    @PluginProperty(group = "advanced")
    private Property<String> name;

    @Schema(
        title = "Organization ID",
        description = "New organization ID this person belongs to"
    )
    @PluginProperty(group = "advanced")
    private Property<Integer> orgId;

    @Schema(
        title = "Owner ID",
        description = "New owner (user ID) for this person"
    )
    @PluginProperty(group = "advanced")
    private Property<Integer> ownerId;

    @Schema(
        title = "Email addresses",
        description = "New list of email addresses for this person"
    )
    @PluginProperty(group = "advanced")
    private Property<List<Map<String, Object>>> emails;

    @Schema(
        title = "Phone numbers",
        description = "New list of phone numbers for this person"
    )
    @PluginProperty(group = "advanced")
    private Property<List<Map<String, Object>>> phones;

    @Schema(
        title = "Visibility",
        description = "New visibility of the person. Valid values: '1' (owner only), '3' (entire company), or '5' (owner and followers)"
    )
    @PluginProperty(group = "destination")
    private Property<String> visibleTo;

    @Schema(
        title = "Custom fields",
        description = "Map of custom field keys and values to update"
    )
    @PluginProperty(group = "destination")
    private Property<Map<String, Object>> customFields;

    @Override
    public Output run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();

        var rApiToken = this.renderApiToken(runContext);
        var rApiUrl = this.renderApiUrl(runContext);

        var rPersonId = runContext.render(this.personId).as(Integer.class)
            .orElseThrow(() -> new IllegalArgumentException("Person ID is required"));

        if (
            name == null && orgId == null && ownerId == null && emails == null
                && phones == null && visibleTo == null && customFields == null
        ) {
            throw new IllegalArgumentException("At least one field must be provided to update the person");
        }

        String rName = name == null ? null : runContext.render(name).as(String.class).orElse(null);
        Integer rOrgId = orgId == null ? null : runContext.render(orgId).as(Integer.class).orElse(null);
        Integer rOwnerId = ownerId == null ? null : runContext.render(ownerId).as(Integer.class).orElse(null);
        String rVisibleTo = visibleTo == null ? null : runContext.render(visibleTo).as(String.class).orElse(null);
        List<Map<String, Object>> rEmails = emails == null ? null : runContext.render(emails).asList(Map.class);
        List<Map<String, Object>> rPhones = phones == null ? null : runContext.render(phones).asList(Map.class);
        Map<String, Object> rCustomFields = customFields == null ? null : runContext.render(customFields).asMap(String.class, Object.class);

        try (PipedriveClient client = new PipedriveClient(runContext, rApiToken, rApiUrl)) {
            Person.PersonBuilder personBuilder = Person.builder();

            if (rName != null) {
                personBuilder.name(rName);
            }

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
                    emailInfos.add(
                        Person.EmailInfo.builder()
                            .value((String) email.get("value"))
                            .primary((Boolean) email.getOrDefault("primary", false))
                            .label((String) email.getOrDefault("label", "work"))
                            .build()
                    );
                }
                personBuilder.emails(emailInfos);
            }

            if (rPhones != null) {
                List<Person.PhoneInfo> phoneInfos = new ArrayList<>();
                for (Map<String, Object> phone : rPhones) {
                    phoneInfos.add(
                        Person.PhoneInfo.builder()
                            .value((String) phone.get("value"))
                            .primary((Boolean) phone.getOrDefault("primary", false))
                            .label((String) phone.getOrDefault("label", "work"))
                            .build()
                    );
                }
                personBuilder.phones(phoneInfos);
            }

            if (rCustomFields != null) {
                personBuilder.customFields(rCustomFields);
            }

            Person person = personBuilder.build();

            logger.info("Updating Pipedrive person with ID: {}", rPersonId);

            PipedriveResponse<Person> response = client.patch(
                "/persons/" + rPersonId, person,
                new TypeReference<>() {
                }
            );

            if (!Boolean.TRUE.equals(response.getSuccess())) {
                throw new IllegalStateException("Failed to update person: " + response.getError());
            }

            var updatedPerson = response.getData();

            logger.info("Successfully updated person: {}", updatedPerson.getId());

            return Output.builder()
                .personId(updatedPerson.getId())
                .updateTime(updatedPerson.getUpdateTime())
                .build();
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Person ID")
        private final Integer personId;

        @Schema(title = "Last update time", description = "Timestamp when the person was last updated")
        private final String updateTime;
    }
}
