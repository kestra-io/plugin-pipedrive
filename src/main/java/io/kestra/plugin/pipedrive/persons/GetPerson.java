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

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Get a person from Pipedrive CRM",
    description = "Retrieves detailed information about a specific person from Pipedrive by their ID."
)
@Plugin(
    examples = {
        @Example(
            title = "Get a person by ID",
            full = true,
            code = """
                id: pipedrive_get_person
                namespace: company.team
                
                tasks:
                  - id: get_person
                    type: io.kestra.plugin.pipedrive.persons.GetPerson
                    apiToken: "{{ secret('PIPEDRIVE_API_TOKEN') }}"
                    personId: 123
                """
        )
    }
)
public class GetPerson extends Task implements RunnableTask<GetPerson.Output> {

    @Schema(
        title = "Pipedrive API token",
        description = "Your Pipedrive API token for authentication"
    )
    @PluginProperty(dynamic = true)
    private Property<String> apiToken;

    @Schema(
        title = "Person ID",
        description = "The ID of the person to retrieve"
    )
    @PluginProperty(dynamic = true)
    private Property<Integer> personId;

    @Override
    public Output run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();
        
        String token = runContext.render(this.apiToken).as(String.class)
            .orElseThrow(() -> new IllegalArgumentException("API token is required"));
        
        Integer id = runContext.render(this.personId).as(Integer.class)
            .orElseThrow(() -> new IllegalArgumentException("Person ID is required"));

        PipedriveClient client = new PipedriveClient(token, logger);

        try {
            logger.info("Fetching person with ID: {}", id);
            
            PipedriveResponse<Person> response = client.get("/persons/" + id, 
                new TypeReference<PipedriveResponse<Person>>() {});

            if (!response.getSuccess()) {
                throw new RuntimeException("Failed to get person: " + response.getError());
            }

            Person person = response.getData();
            
            logger.info("Successfully retrieved person: {}", person.getName());

            return Output.builder()
                .personId(person.getId())
                .name(person.getName())
                .firstName(person.getFirstName())
                .lastName(person.getLastName())
                .email(person.getEmail() != null && !person.getEmail().isEmpty() 
                    ? person.getEmail().get(0).getValue() : null)
                .phone(person.getPhone() != null && !person.getPhone().isEmpty() 
                    ? person.getPhone().get(0).getValue() : null)
                .orgId(person.getOrgId())
                .ownerId(person.getOwnerId())
                .build();
        } finally {
            client.close();
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Person ID")
        private final Integer personId;

        @Schema(title = "Full name")
        private final String name;

        @Schema(title = "First name")
        private final String firstName;

        @Schema(title = "Last name")
        private final String lastName;

        @Schema(title = "Primary email")
        private final String email;

        @Schema(title = "Primary phone")
        private final String phone;

        @Schema(title = "Organization ID")
        private final Integer orgId;

        @Schema(title = "Owner user ID")
        private final Integer ownerId;
    }
}
