# 环境变量配置说明

## 必需的环境变量

### DASHSCOPE_API_KEY
- **说明**: 阿里云 DashScope API 密钥
- **获取方式**: 访问 [DashScope 控制台](https://dashscope.console.aliyun.com/) 获取
- **必需**: 是

## 可选的环境变量

### TAVILY_API_KEY
- **说明**: Tavily 搜索 API 密钥
- **获取方式**: 访问 [Tavily](https://tavily.com/) 注册并获取
- **必需**: 否（如果不配置，网络搜索功能将不可用）

## 配置方法

### 方法 1: 系统环境变量（推荐）

**Windows:**
```cmd
setx DASHSCOPE_API_KEY "your_api_key_here"
setx TAVILY_API_KEY "your_api_key_here"
```

**Linux/Mac:**
```bash
export DASHSCOPE_API_KEY="your_api_key_here"
export TAVILY_API_KEY="your_api_key_here"
```

### 方法 2: IDE 配置

在 IDEA 中配置运行环境变量：
1. Run -> Edit Configurations
2. 选择 Spring Boot 应用
3. Environment variables 中添加：
   ```
   DASHSCOPE_API_KEY=your_api_key_here;TAVILY_API_KEY=your_api_key_here
   ```

### 方法 3: application.yml 直接配置（不推荐）

直接在 `src/main/resources/application.yml` 中修改：
```yaml
spring:
  ai:
    dashscope:
      api-key: your_api_key_here

tavily:
  api-key: your_api_key_here
```

**注意**: 此方法会将密钥提交到版本控制，存在安全风险。

## 启动应用

配置好环境变量后，运行：
```bash
mvn spring-boot:run
```

或在 IDEA 中直接运行 `Rin1AiAgentApplication` 主类。
