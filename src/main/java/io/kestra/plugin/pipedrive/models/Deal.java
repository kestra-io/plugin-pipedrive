package io.kestra.plugin.pipedrive.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
@JsonIgnoreProperties(ignoreUnknown = true)
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

    @JsonProperty("local_won_date")
    private String localWonDate;

    @JsonProperty("local_lost_date")
    private String localLostDate;

    @JsonProperty("local_close_date")
    private String localCloseDate;

    @JsonProperty("origin")
    private String origin;

    @JsonProperty("origin_id")
    private String originId;

    @JsonProperty("channel")
    private Integer channel;

    @JsonProperty("channel_id")
    private String channelId;

    @JsonProperty("acv")
    private BigDecimal acv;

    @JsonProperty("arr")
    private BigDecimal arr;

    @JsonProperty("mrr")
    private BigDecimal mrr;

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
