package com.lin.rin1aiagent.mcp.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
public class McpResponse {
    @JsonProperty("jsonrpc")
    private String jsonrpc;

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("result")
    private Map<String, Object> result;

    @JsonProperty("error")
    private McpError error;

    @Data
    @NoArgsConstructor
    public static class McpError {
        @JsonProperty("code")
        private Integer code;

        @JsonProperty("message")
        private String message;

        @JsonProperty("data")
        private Object data;
    }
}
