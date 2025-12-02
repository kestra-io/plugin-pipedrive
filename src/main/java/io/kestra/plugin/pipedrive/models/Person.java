package io.kestra.plugin.pipedrive.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Represents a Person in Pipedrive CRM
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Person {
    @JsonProperty("id")
    private Integer id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("last_name")
    private String lastName;

    @JsonProperty("emails")
    private List<EmailInfo> emails;

    @JsonProperty("phones")
    private List<PhoneInfo> phones;

    @JsonProperty("org_id")
    private Integer orgId;

    @JsonProperty("owner_id")
    private Integer ownerId;

    @JsonProperty("visible_to")
    private String visibleTo;

    @JsonProperty("add_time")
    private String addTime;

    @JsonProperty("update_time")
    private String updateTime;

    @JsonProperty("custom_fields")
    private Map<String, Object> customFields;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class EmailInfo {
        @JsonProperty("value")
        private String value;

        @JsonProperty("primary")
        private Boolean primary;

        @JsonProperty("label")
        private String label;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PhoneInfo {
        @JsonProperty("value")
        private String value;

        @JsonProperty("primary")
        private Boolean primary;

        @JsonProperty("label")
        private String label;
    }
}
