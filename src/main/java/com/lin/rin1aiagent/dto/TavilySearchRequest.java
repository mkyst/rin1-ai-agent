package com.lin.rin1aiagent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TavilySearchRequest {
    @JsonProperty("api_key")
    private String apiKey;

    @JsonProperty("query")
    private String query;

    @JsonProperty("max_results")
    private Integer maxResults = 5;
}
