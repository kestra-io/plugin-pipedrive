package io.kestra.plugin.pipedrive.persons;

import com.fasterxml.jackson.core.type.TypeReference;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.pipedrive.client.PipedriveClient;
import io.kestra.plugin.pipedrive.models.Person;
import io.kestra.plugin.pipedrive.models.PipedriveResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode
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
                    type: io.kestra.plugin.pipedrive.persons.CreatePerson
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
                    type: io.kestra.plugin.pipedrive.persons.CreatePerson
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
public class CreatePerson extends Task implements RunnableTask<CreatePerson.Output> {

    @Schema(
        title = "Pipedrive API token",
        description = "Your Pipedrive API token for authentication. Find it in your Pipedrive settings under API."
    )
    @PluginProperty(dynamic = true)
    private Property<String> apiToken;

    @Schema(
        title = "Person name",
        description = "Full name of the person"
    )
    @PluginProperty(dynamic = true)
    private Property<String> name;

    @Schema(
        title = "Organization ID",
        description = "ID of the organization this person belongs to"
    )
    @PluginProperty(dynamic = true)
    private Property<Integer> orgId;

    @Schema(
        title = "Owner ID",
        description = "ID of the user who will be marked as the owner of this person"
    )
    @PluginProperty(dynamic = true)
    private Property<Integer> ownerId;

    @Schema(
        title = "Email addresses",
        description = "List of email addresses for this person"
    )
    @PluginProperty(dynamic = true)
    private Property<List<Map<String, Object>>> emails;

    @Schema(
        title = "Phone numbers",
        description = "List of phone numbers for this person"
    )
    @PluginProperty(dynamic = true)
    private Property<List<Map<String, Object>>> phones;

    @Schema(
        title = "Visibility",
        description = "Visibility of the person. Valid values: '1' (owner only), '3' (entire company), or '5' (owner and followers)"
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
        
        String personName = runContext.render(this.name).as(String.class)
            .orElseThrow(() -> new IllegalArgumentException("Person name is required"));

        PipedriveClient client = new PipedriveClient(token, logger);

        try {
            // Build person object
            Person.PersonBuilder personBuilder = Person.builder()
                .name(personName);

            // Add optional fields
            if (orgId != null) {
                personBuilder.orgId(runContext.render(orgId).as(Integer.class).orElse(null));
            }

            if (ownerId != null) {
                personBuilder.ownerId(runContext.render(ownerId).as(Integer.class).orElse(null));
            }

            if (visibleTo != null) {
                personBuilder.visibleTo(runContext.render(visibleTo).as(String.class).orElse(null));
            }

            // Add emails
            if (emails != null) {
                List<Map<String, Object>> emailList = runContext.render(emails).asList(Map.class);
                List<Person.EmailInfo> emailInfos = new ArrayList<>();
                for (Map<String, Object> email : emailList) {
                    emailInfos.add(Person.EmailInfo.builder()
                        .value((String) email.get("value"))
                        .primary((Boolean) email.getOrDefault("primary", false))
                        .label((String) email.getOrDefault("label", "work"))
                        .build());
                }
                personBuilder.email(emailInfos);
            }

            // Add phones
            if (phones != null) {
                List<Map<String, Object>> phoneList = runContext.render(phones).asList(Map.class);
                List<Person.PhoneInfo> phoneInfos = new ArrayList<>();
                for (Map<String, Object> phone : phoneList) {
                    phoneInfos.add(Person.PhoneInfo.builder()
                        .value((String) phone.get("value"))
                        .primary((Boolean) phone.getOrDefault("primary", false))
                        .label((String) phone.getOrDefault("label", "work"))
                        .build());
                }
                personBuilder.phone(phoneInfos);
            }

            // Add custom fields
            if (customFields != null) {
                personBuilder.customFields(runContext.render(customFields).asMap(String.class, Object.class));
            }

            Person person = personBuilder.build();

            logger.info("Creating person in Pipedrive: {}", personName);
            
            PipedriveResponse<Person> response = client.post("/persons", person, 
                new TypeReference<PipedriveResponse<Person>>() {});

            if (!response.getSuccess()) {
                throw new RuntimeException("Failed to create person: " + response.getError());
            }

            Person createdPerson = response.getData();
            
            logger.info("Successfully created person with ID: {}", createdPerson.getId());

            return Output.builder()
                .personId(createdPerson.getId())
                .name(createdPerson.getName())
                .email(createdPerson.getEmail() != null && !createdPerson.getEmail().isEmpty() 
                    ? createdPerson.getEmail().get(0).getValue() : null)
                .build();
        } finally {
            client.close();
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
            title = "Person name",
            description = "The name of the created person"
        )
        private final String name;

        @Schema(
            title = "Email",
            description = "Primary email address of the created person"
        )
        private final String email;
    }
}
