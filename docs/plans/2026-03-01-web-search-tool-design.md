# 网页搜索工具设计文档

## 概述

为 rin1-ai-agent 项目添加网页搜索功能，使 AI Agent 能够获取实时网络信息。

## 技术选型

**搜索引擎：** Tavily Search API
- 专为 AI 搜索设计
- 返回结果质量高
- 有免费额度
- 提供官方 Java SDK

**实现方式：** 使用 Tavily Java SDK（方案 1）
- 类型安全，易于维护
- 与现有 FileOperationTool 风格一致
- 官方 SDK 提供完善的错误处理

## 架构设计

### 核心组件

**类名：** `WebSearchTool`
**位置：** `src/main/java/com/lin/rin1aiagent/tools/WebSearchTool.java`

**主要方法：**
```java
@Tool(description = "Search the web for information")
public String search(@ToolParam(description = "Search query") String query)
```

### 配置管理

**配置文件：** `application.yml`
```yaml
tavily:
  api-key: ${TAVILY_API_KEY}
```

**配置注入：**
- 使用 `@Value("${tavily.api-key}")` 注入 API key
- 支持环境变量配置

### 依赖管理

**Maven 依赖：** 在 `pom.xml` 中添加
```xml
<dependency>
    <groupId>com.tavily</groupId>
    <artifactId>tavily-java</artifactId>
    <version>[最新版本]</version>
</dependency>
```

## 数据流设计

### 搜索流程

1. AI Agent 调用 `search(String query)` 方法
2. WebSearchTool 验证查询参数
3. 使用 Tavily SDK 发送搜索请求
4. 接收搜索结果（标题、URL、摘要）
5. 格式化结果为易读文本
6. 返回给 AI Agent

### 返回格式

```
搜索结果：
1. [标题]
   链接: [URL]
   摘要: [内容摘要]

2. [标题]
   链接: [URL]
   摘要: [内容摘要]
...
```

## 错误处理

### 异常场景

1. API key 未配置或无效
2. 网络请求失败
3. API 限流或配额用尽
4. 搜索查询为空或无效

### 处理策略

- 捕获所有异常，返回友好错误信息
- 记录详细错误日志
- 返回格式：`"搜索失败: [错误原因]"`

### 参数验证

- 检查查询字符串是否为空
- 限制查询长度（避免过长请求）

## 实现细节

### 类结构

```java
public class WebSearchTool {
    @Value("${tavily.api-key}")
    private String apiKey;

    @Tool(description = "Search the web for information")
    public String search(@ToolParam(description = "Search query") String query) {
        // 1. 参数验证
        // 2. 调用 Tavily API
        // 3. 格式化结果
        // 4. 错误处理
    }
}
```

### 结果限制

- 默认返回前 5 条搜索结果
- 每条结果包含：标题、链接、摘要
- 总字符数控制在合理范围内

## 测试计划

1. 单元测试：验证参数验证逻辑
2. 集成测试：使用真实 API key 测试搜索功能
3. 错误场景测试：测试各种异常情况的处理

## 部署要求

1. 配置 `TAVILY_API_KEY` 环境变量
2. 确保网络可访问 Tavily API
3. 在 AI Agent 配置中注册 WebSearchTool

## 维护考虑

- API key 安全存储（不提交到代码仓库）
- 监控 API 调用次数和配额使用
- 定期更新 Tavily SDK 版本
