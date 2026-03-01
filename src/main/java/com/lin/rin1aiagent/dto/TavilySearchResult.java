package com.lin.rin1aiagent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TavilySearchResult {
    @JsonProperty("title")
    private String title;

    @JsonProperty("url")
    private String url;

    @JsonProperty("content")
    private String content;

    @JsonProperty("score")
    private Double score;
}
