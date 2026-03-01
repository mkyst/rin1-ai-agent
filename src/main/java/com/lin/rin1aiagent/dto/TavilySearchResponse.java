package com.lin.rin1aiagent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class TavilySearchResponse {
    @JsonProperty("query")
    private String query;

    @JsonProperty("results")
    private List<TavilySearchResult> results;

    @JsonProperty("answer")
    private String answer;
}
