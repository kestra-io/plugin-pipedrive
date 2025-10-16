package io.kestra.plugin.pipedrive.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * Generic response wrapper for Pipedrive API responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PipedriveResponse<T> {
    @JsonProperty("success")
    private Boolean success;

    @JsonProperty("data")
    private T data;

    @JsonProperty("error")
    private String error;

    @JsonProperty("error_info")
    private String errorInfo;

    @JsonProperty("additional_data")
    private Object additionalData;
}
