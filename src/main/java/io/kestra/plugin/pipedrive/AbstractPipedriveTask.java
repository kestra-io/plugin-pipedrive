package io.kestra.plugin.pipedrive;

import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.pipedrive.client.PipedriveClient;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * Shared configuration and utilities for Pipedrive tasks.
 */
@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractPipedriveTask extends Task {
    @Schema(
        title = "Pipedrive API token",
        description = "Your Pipedrive API token for authentication."
    )
    @NotNull
    protected Property<String> apiToken;

    @Schema(
        title = "Pipedrive API URL",
        description = "Base URL for the Pipedrive API. Override for testing purposes."
    )
    @NotNull
    @Builder.Default
    protected Property<String> apiUrl = Property.ofValue(PipedriveClient.DEFAULT_BASE_URL);

    protected String renderApiToken(RunContext runContext) throws IllegalArgumentException, IllegalVariableEvaluationException {
        return runContext.render(this.apiToken).as(String.class)
            .orElseThrow(() -> new IllegalArgumentException("API token is required"));
    }

    protected String renderApiUrl(RunContext runContext) throws IllegalVariableEvaluationException {
        return runContext.render(this.apiUrl).as(String.class).orElse(PipedriveClient.DEFAULT_BASE_URL);
    }
}
