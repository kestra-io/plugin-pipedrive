package io.kestra.plugin.pipedrive.notes;

import com.fasterxml.jackson.core.type.TypeReference;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.pipedrive.client.PipedriveClient;
import io.kestra.plugin.pipedrive.models.Note;
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
    title = "Add a note in Pipedrive CRM",
    description = "Creates a new note attached to a deal, person, or organization in Pipedrive."
)
@Plugin(
    examples = {
        @Example(
            title = "Add a note to a deal",
            full = true,
            code = """
                id: pipedrive_add_note_to_deal
                namespace: company.team
                
                tasks:
                  - id: add_note
                    type: io.kestra.plugin.pipedrive.notes.AddNote
                    apiToken: "{{ secret('PIPEDRIVE_API_TOKEN') }}"
                    content: "Customer requested a follow-up call next week"
                    dealId: 123
                """
        ),
        @Example(
            title = "Add a note to a person",
            full = true,
            code = """
                id: pipedrive_add_note_to_person
                namespace: company.team
                
                tasks:
                  - id: add_note
                    type: io.kestra.plugin.pipedrive.notes.AddNote
                    apiToken: "{{ secret('PIPEDRIVE_API_TOKEN') }}"
                    content: "Met at conference, interested in our enterprise plan"
                    personId: 456
                    pinnedToPersonFlag: true
                """
        )
    }
)
public class AddNote extends Task implements RunnableTask<AddNote.Output> {

    @Schema(
        title = "Pipedrive API token",
        description = "Your Pipedrive API token for authentication"
    )
    @PluginProperty(dynamic = true)
    private Property<String> apiToken;

    @Schema(
        title = "Note content",
        description = "The content of the note"
    )
    @PluginProperty(dynamic = true)
    private Property<String> content;

    @Schema(
        title = "Deal ID",
        description = "ID of the deal this note is attached to"
    )
    @PluginProperty(dynamic = true)
    private Property<Integer> dealId;

    @Schema(
        title = "Person ID",
        description = "ID of the person this note is attached to"
    )
    @PluginProperty(dynamic = true)
    private Property<Integer> personId;

    @Schema(
        title = "Organization ID",
        description = "ID of the organization this note is attached to"
    )
    @PluginProperty(dynamic = true)
    private Property<Integer> orgId;

    @Schema(
        title = "Pin to deal",
        description = "Whether to pin this note to the deal"
    )
    @PluginProperty(dynamic = true)
    private Property<Boolean> pinnedToDealFlag;

    @Schema(
        title = "Pin to person",
        description = "Whether to pin this note to the person"
    )
    @PluginProperty(dynamic = true)
    private Property<Boolean> pinnedToPersonFlag;

    @Schema(
        title = "Pin to organization",
        description = "Whether to pin this note to the organization"
    )
    @PluginProperty(dynamic = true)
    private Property<Boolean> pinnedToOrganizationFlag;

    @Override
    public Output run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();
        
        String token = runContext.render(this.apiToken).as(String.class)
            .orElseThrow(() -> new IllegalArgumentException("API token is required"));
        
        String noteContent = runContext.render(this.content).as(String.class)
            .orElseThrow(() -> new IllegalArgumentException("Note content is required"));

        PipedriveClient client = new PipedriveClient(token, logger);

        try {
            Note.NoteBuilder noteBuilder = Note.builder()
                .content(noteContent);

            // Add optional fields
            if (dealId != null) {
                noteBuilder.dealId(runContext.render(dealId).as(Integer.class).orElse(null));
            }

            if (personId != null) {
                noteBuilder.personId(runContext.render(personId).as(Integer.class).orElse(null));
            }

            if (orgId != null) {
                noteBuilder.orgId(runContext.render(orgId).as(Integer.class).orElse(null));
            }

            if (pinnedToDealFlag != null) {
                noteBuilder.pinnedToDealFlag(runContext.render(pinnedToDealFlag).as(Boolean.class).orElse(false));
            }

            if (pinnedToPersonFlag != null) {
                noteBuilder.pinnedToPersonFlag(runContext.render(pinnedToPersonFlag).as(Boolean.class).orElse(false));
            }

            if (pinnedToOrganizationFlag != null) {
                noteBuilder.pinnedToOrganizationFlag(runContext.render(pinnedToOrganizationFlag).as(Boolean.class).orElse(false));
            }

            Note note = noteBuilder.build();

            logger.info("Adding note to Pipedrive");
            
            PipedriveResponse<Note> response = client.post("/notes", note, 
                new TypeReference<PipedriveResponse<Note>>() {});

            if (!response.getSuccess()) {
                throw new RuntimeException("Failed to add note: " + response.getError());
            }

            Note createdNote = response.getData();
            
            logger.info("Successfully created note with ID: {}", createdNote.getId());

            return Output.builder()
                .noteId(createdNote.getId())
                .content(createdNote.getContent())
                .dealId(createdNote.getDealId())
                .personId(createdNote.getPersonId())
                .orgId(createdNote.getOrgId())
                .build();
        } finally {
            client.close();
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Note ID", description = "The ID of the created note")
        private final Integer noteId;

        @Schema(title = "Content", description = "The content of the note")
        private final String content;

        @Schema(title = "Deal ID", description = "ID of the associated deal")
        private final Integer dealId;

        @Schema(title = "Person ID", description = "ID of the associated person")
        private final Integer personId;

        @Schema(title = "Organization ID", description = "ID of the associated organization")
        private final Integer orgId;
    }
}
