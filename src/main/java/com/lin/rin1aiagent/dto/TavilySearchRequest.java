package com.lin.rin1aiagent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TavilySearchRequest {
    @JsonProperty("api_key")
    @NotBlank
    private String apiKey;

    @JsonProperty("query")
    @NotBlank
    private String query;

    @JsonProperty("max_results")
    @Min(1)
    @Max(10)
    private Integer maxResults = 5;
}
