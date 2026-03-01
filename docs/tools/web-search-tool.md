# WebSearchTool 使用文档

## 概述

WebSearchTool 提供网页搜索功能，使 AI Agent 能够获取实时网络信息。

## 配置

### 环境变量

```bash
export TAVILY_API_KEY=your_tavily_api_key
```

### application.yml

```yaml
tavily:
  api-key: ${TAVILY_API_KEY}
  api-url: https://api.tavily.com/search
```

## 使用方法

### 在 AI Agent 中使用

```java
@Bean
public ChatClient chatClient(ChatClient.Builder builder, WebSearchTool webSearchTool) {
    return builder
        .defaultTools(webSearchTool)
        .build();
}
```

### 直接调用

```java
@Autowired
private WebSearchTool webSearchTool;

public void search() {
    String result = webSearchTool.search("Java Spring Boot");
    System.out.println(result);
}
```

## 返回格式

```
搜索结果：

1. [标题]
   链接: [URL]
   摘要: [内容摘要]

2. [标题]
   链接: [URL]
   摘要: [内容摘要]
```

## 错误处理

- 查询为空: 返回 "搜索失败: 查询不能为空"
- 查询过长: 返回 "搜索失败: 查询长度不能超过400个字符"
- API 错误: 返回 "搜索失败: [错误信息]"
- 无结果: 返回 "未找到相关搜索结果"

## 限制

- 每次搜索返回最多 5 条结果
- 查询长度限制 400 字符
- 依赖 Tavily API 配额限制

## 获取 API Key

1. 访问 https://tavily.com
2. 注册账号
3. 获取免费 API key（每月 1000 次免费调用）
