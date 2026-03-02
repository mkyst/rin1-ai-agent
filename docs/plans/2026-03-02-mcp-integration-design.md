# MCP Integration Design

## Overview

This document describes the design for integrating MCP (Model Context Protocol) servers into the rin1-ai-agent application, specifically focusing on the Amap Maps service integration.

## Problem Statement

The `LoveApp.doChatWithMcp()` method requires a `ToolCallbackProvider` bean that doesn't exist in the Spring context. The application has an `mcp-servers.json` configuration file defining MCP servers, but lacks the infrastructure to connect to these servers and expose their tools to the AI chat client.

## Goals

1. Enable the application to connect to MCP servers via stdio communication
2. Discover and register MCP tools as Spring AI functions
3. Fix the failing `LoveAppTest.doChatWithMcp()` test
4. Support the Amap Maps MCP server for location-based queries

## Non-Goals

- Supporting the image search MCP server (deferred)
- Building a generic MCP framework for multiple servers
- Implementing MCP server-side functionality

## Architecture

### Component Overview

```
┌─────────────────┐
│   LoveApp       │
│  (ChatClient)   │
└────────┬────────┘
         │ uses
         ▼
┌─────────────────────────┐
│ ToolCallbackProvider    │
│ (Spring Bean)           │
└────────┬────────────────┘
         │ contains
         ▼
┌─────────────────────────┐
│  MCP Tool Functions     │
│  (wrapped)              │
└────────┬────────────────┘
         │ delegates to
         ▼
┌─────────────────────────┐
│     McpClient           │
│  (Process Manager +     │
│   JSON-RPC Client)      │
└────────┬────────────────┘
         │ stdio
         ▼
┌─────────────────────────┐
│  MCP Server Process     │
│  (npx @amap/...)        │
└─────────────────────────┘
```

### Core Components

**1. McpClient**
- Manages MCP server process lifecycle
- Handles stdin/stdout communication
- Implements JSON-RPC 2.0 protocol
- Methods:
  - `initialize()` - Start process and send initialize request
  - `listTools()` - Discover available tools
  - `callTool(name, arguments)` - Execute tool with parameters
  - `shutdown()` - Clean up process

**2. McpToolCallbackProvider**
- Spring `@Configuration` class
- Reads `mcp-servers.json` configuration
- Creates and initializes `McpClient` for Amap Maps
- Converts MCP tools to Spring AI Functions
- Provides `ToolCallbackProvider` bean
- Manages lifecycle with `@PreDestroy`

**3. Data Models**
- `McpServerConfig` - Server configuration (command, args, env)
- `McpToolDefinition` - Tool metadata (name, description, inputSchema)
- `McpRequest` - JSON-RPC request wrapper
- `McpResponse` - JSON-RPC response wrapper

## Implementation Details

### MCP Communication Protocol

**Request Format (JSON-RPC 2.0)**:
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "tools/list",
  "params": {}
}
```

**Response Format**:
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "tools": [
      {
        "name": "search_nearby",
        "description": "Search for nearby places",
        "inputSchema": { ... }
      }
    ]
  }
}
```

### Process Management

- Use `ProcessBuilder` to launch MCP server
- Command: `npx.cmd -y @amap/amap-maps-mcp-server`
- Environment: `AMAP_MAPS_API_KEY` from config
- Communication: stdin (write) / stdout (read)
- Timeout: 30 seconds for each request

### Tool Registration

1. Call `McpClient.listTools()` to get tool definitions
2. For each tool, create a Spring AI Function:
   - Name: tool.name
   - Description: tool.description
   - Input: Parse JSON Schema to determine parameter types
   - Execution: Call `McpClient.callTool(name, args)`
3. Register all functions with `ToolCallbackProvider`

### Error Handling

**Graceful Degradation**:
- If MCP process fails to start: log error, return empty tool list
- If tool discovery fails: log error, continue with empty tools
- If tool execution fails: return error message to AI

**Timeouts**:
- Process startup: 10 seconds
- Tool discovery: 30 seconds
- Tool execution: 30 seconds

**Logging**:
- Log all MCP requests/responses at DEBUG level
- Log errors at ERROR level with full stack traces

## Configuration

### mcp-servers.json Structure

```json
{
  "mcpServers": {
    "amap-maps": {
      "command": "npx.cmd",
      "args": ["-y", "@amap/amap-maps-mcp-server"],
      "env": {
        "AMAP_MAPS_API_KEY": "5095196bafba119fc09d39d5c5ffe5da"
      }
    }
  }
}
```

Only the `amap-maps` server will be initialized in this implementation.

## Testing Strategy

### Unit Tests

**McpClientTest**:
- Test JSON-RPC request/response serialization
- Mock process streams to verify protocol
- Test error handling for malformed responses

### Integration Tests

**McpToolCallbackProviderTest**:
- Verify bean creation
- Verify tool registration
- Test with real MCP server process (requires Node.js)

**LoveAppTest.doChatWithMcp()**:
- End-to-end test with real AI chat
- Verify Amap Maps tools are callable
- Validate response contains location data

## Dependencies

No new Maven dependencies required. Using existing:
- Jackson for JSON parsing
- Spring Framework for DI
- Java ProcessBuilder for process management

## Risks and Mitigations

**Risk**: MCP server process crashes during operation
**Mitigation**: Implement health checks and auto-restart logic (future enhancement)

**Risk**: Node.js/npx not available in deployment environment
**Mitigation**: Document Node.js as a runtime requirement; graceful degradation if unavailable

**Risk**: API key exposure in configuration file
**Mitigation**: Document that mcp-servers.json should not be committed with real keys; support environment variable substitution

## Future Enhancements

1. Support multiple MCP servers simultaneously
2. Dynamic tool reloading without restart
3. MCP server health monitoring
4. Caching of tool definitions
5. Support for image search MCP server

## Success Criteria

1. `LoveAppTest.doChatWithMcp()` test passes
2. AI can successfully call Amap Maps tools
3. No errors in application startup logs
4. Tool calls complete within timeout limits
