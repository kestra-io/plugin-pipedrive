package io.kestra.plugin.pipedrive.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Represents a Deal in Pipedrive CRM
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Deal {
    @JsonProperty("id")
    private Integer id;

    @JsonProperty("title")
    private String title;

    @JsonProperty("value")
    private BigDecimal value;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("user_id")
    private Integer userId;

    @JsonProperty("person_id")
    private Integer personId;

    @JsonProperty("org_id")
    private Integer orgId;

    @JsonProperty("stage_id")
    private Integer stageId;

    @JsonProperty("pipeline_id")
    private Integer pipelineId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("probability")
    private Double probability;

    @JsonProperty("expected_close_date")
    private String expectedCloseDate;

    @JsonProperty("close_time")
    private String closeTime;

    @JsonProperty("won_time")
    private String wonTime;

    @JsonProperty("lost_time")
    private String lostTime;

    @JsonProperty("lost_reason")
    private String lostReason;

    @JsonProperty("visible_to")
    private String visibleTo;

    @JsonProperty("add_time")
    private String addTime;

    @JsonProperty("update_time")
    private String updateTime;

    @JsonProperty("custom_fields")
    private Map<String, Object> customFields;
}
