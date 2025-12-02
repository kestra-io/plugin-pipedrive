package io.kestra.plugin.pipedrive.persons;

import com.fasterxml.jackson.core.type.TypeReference;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.common.FetchType;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.FileSerde;
import io.kestra.plugin.pipedrive.AbstractPipedriveTask;
import io.kestra.plugin.pipedrive.client.PipedriveClient;
import io.kestra.plugin.pipedrive.models.Person;
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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.net.URI;
import java.util.List;

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
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
                    type: io.kestra.plugin.pipedrive.persons.Get
                    apiToken: "{{ secret('PIPEDRIVE_API_TOKEN') }}"
                    personId: 123
                """
        )
    }
)
public class Get extends AbstractPipedriveTask implements RunnableTask<Get.Output> {

    @Schema(
        title = "Person ID",
        description = "The ID of the person to retrieve"
    )
    @NotNull
    private Property<Integer> personId;

    @Schema(
        title = "Fetch strategy",
        description = "How to fetch the person data (fetch, fetch one or store)"
    )
    @NotNull
    @Builder.Default
    private Property<FetchType> fetchType = Property.ofValue(FetchType.FETCH_ONE);

    @Override
    public Output run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();

        String rApiToken = this.renderApiToken(runContext);
        String rApiUrl = this.renderApiUrl(runContext);

        Integer rPersonId = runContext.render(this.personId).as(Integer.class)
            .orElseThrow(() -> new IllegalArgumentException("Person ID is required"));

        try (PipedriveClient client = new PipedriveClient(runContext, rApiToken, rApiUrl)) {
            logger.info("Fetching person with ID: {}", rPersonId);

            PipedriveResponse<Person> response = client.get("/persons/" + rPersonId,
                new TypeReference<>() {
                });

            if (!Boolean.TRUE.equals(response.getSuccess())) {
                throw new IllegalStateException("Failed to get person: " + response.getError());
            }

            Person person = response.getData();

            logger.info("Successfully retrieved person: {}", person.getName());

            FetchType rFetchType = runContext.render(this.fetchType).as(FetchType.class).orElse(FetchType.FETCH_ONE);

            return switch (rFetchType) {
                case STORE -> {
                    java.io.File tempFile = runContext.workingDir().createTempFile(".ion").toFile();
                    try (BufferedWriter output = new BufferedWriter(new FileWriter(tempFile), FileSerde.BUFFER_SIZE)) {
                        Long count = FileSerde.writeAll(output, reactor.core.publisher.Flux.just(person)).block();
                        URI uri = runContext.storage().putFile(tempFile);
                        yield Output.builder()
                            .uri(uri)
                            .count(count != null ? count.intValue() : 0)
                            .build();
                    }
                }
                case FETCH -> Output.builder()
                    .persons(List.of(person))
                    .count(1)
                    .build();
                case FETCH_ONE -> Output.builder()
                    .person(person)
                    .count(1)
                    .build();
                default -> Output.builder()
                    .person(person)
                    .count(1)
                    .build();
            };
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Person", description = "The retrieved person")
        private final Person person;

        @Schema(title = "Persons", description = "List of retrieved persons when fetchType is FETCH")
        private final List<Person> persons;

        @Schema(title = "URI", description = "Stored persons location when fetchType is STORE")
        private final URI uri;

        @Schema(title = "Count", description = "Number of persons retrieved or stored")
        private final Integer count;
    }
}
