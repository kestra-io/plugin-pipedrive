package io.kestra.plugin.pipedrive.persons;

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
    title = "Delete a person from Pipedrive CRM",
    description = """
        Deletes a person in Pipedrive. Pipedrive soft-deletes the record: it is moved to the trash and only \
        purged permanently after 30 days, during which it can still be restored from the Pipedrive UI."""
)
@Plugin(
    examples = {
        @Example(
            title = "Delete a person by ID",
            full = true,
            code = """
                id: pipedrive_delete_person
                namespace: company.team

                tasks:
                  - id: delete_person
                    type: io.kestra.plugin.pipedrive.persons.Delete
                    apiToken: "{{ secret('PIPEDRIVE_API_TOKEN') }}"
                    personId: 55
                """
        )
    }
)
public class Delete extends AbstractPipedriveTask implements RunnableTask<Delete.Output> {

    @Schema(
        title = "Person ID",
        description = "The ID of the person to delete"
    )
    @NotNull
    @PluginProperty(group = "main")
    private Property<Integer> personId;

    @Override
    public Output run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();

        var rApiToken = this.renderApiToken(runContext);
        var rApiUrl = this.renderApiUrl(runContext);

        var rPersonId = runContext.render(this.personId).as(Integer.class)
            .orElseThrow(() -> new IllegalArgumentException("Person ID is required"));

        try (PipedriveClient client = new PipedriveClient(runContext, rApiToken, rApiUrl)) {
            logger.info("Deleting Pipedrive person with ID: {}", rPersonId);

            PipedriveResponse<Map<String, Object>> response = client.delete(
                "/persons/" + rPersonId,
                new TypeReference<>() {
                }
            );

            if (!Boolean.TRUE.equals(response.getSuccess())) {
                throw new IllegalStateException("Failed to delete person with ID " + rPersonId);
            }

            logger.info("Successfully deleted person with ID: {}", rPersonId);

            return Output.builder()
                .id(rPersonId)
                .deleted(true)
                .build();
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Person ID", description = "The ID of the deleted person")
        private final Integer id;

        @Schema(title = "Deleted", description = "Whether the person was successfully deleted")
        private final Boolean deleted;
    }
}
