package io.kestra.plugin.pipedrive.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * Represents a Note in Pipedrive CRM
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Note {
    @JsonProperty("id")
    private Integer id;

    @JsonProperty("content")
    private String content;

    @JsonProperty("deal_id")
    private Integer dealId;

    @JsonProperty("person_id")
    private Integer personId;

    @JsonProperty("org_id")
    private Integer orgId;

    @JsonProperty("lead_id")
    private String leadId;

    @JsonProperty("user_id")
    private Integer userId;

    @JsonProperty("add_time")
    private String addTime;

    @JsonProperty("update_time")
    private String updateTime;

    @JsonProperty("active_flag")
    private Boolean activeFlag;

    @JsonProperty("pinned_to_deal_flag")
    private Boolean pinnedToDealFlag;

    @JsonProperty("pinned_to_person_flag")
    private Boolean pinnedToPersonFlag;

    @JsonProperty("pinned_to_organization_flag")
    private Boolean pinnedToOrganizationFlag;
}
