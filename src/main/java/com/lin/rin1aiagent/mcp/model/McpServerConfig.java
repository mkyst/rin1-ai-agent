package com.lin.rin1aiagent.mcp.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class McpServerConfig {
    private String command;
    private List<String> args;
    private Map<String, String> env;
}
