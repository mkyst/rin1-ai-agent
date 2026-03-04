已按你的要求把前端里和后端关联的核心代码加了注释，并整理如下。

**后端关联文件清单**
1. 环境配置  
[.env.example](f:/ideaProjects/rin1-ai-agent/fronted/.env.example:1)  
说明：定义前端请求后端的根地址 `VITE_API_BASE_URL`。

2. 后端通信封装（直接请求后端）  
[useSseChat.ts](f:/ideaProjects/rin1-ai-agent/fronted/src/composables/useSseChat.ts:12)  
说明：  
- `API_BASE` 读取后端地址。  
- `STREAM_ENDPOINT_MAP` 映射 `basic/rag/mcp` 到后端 3 个 SSE 接口。  
- `buildUrl()` 统一拼接 `message + chatId` 参数。  
- `streamChat()` 用 `EventSource` 消费后端流式响应。  
- `syncChat()` 调后端同步接口做兜底。

3. 聊天业务状态（间接调用后端）  
[chatStore.ts](f:/ideaProjects/rin1-ai-agent/fronted/src/stores/chatStore.ts:65)  
说明：  
- `mode` 决定请求哪个后端接口。  
- `chatId` 作为后端会话记忆标识。  
- `sendMessage()` 调 `streamChat()` 发起流式请求并拼接分片。  
- `basic` 模式在无流分片时走 `syncChat()` 兜底。  
- `switchSession()` 切换会话后，后续请求自动带新 `chatId`。

4. 页面触发入口（间接关联后端）  
[WorkbenchView.vue](f:/ideaProjects/rin1-ai-agent/fronted/src/views/WorkbenchView.vue:21)  
说明：  
- `handleSend()` 调 `chatStore.sendMessage()`，由 store 进入后端请求链路。  
- `handleModeChange()` 切换模式，影响后端接口选择。

**我已做的“注释解释”**
1. 在 [useSseChat.ts](f:/ideaProjects/rin1-ai-agent/fronted/src/composables/useSseChat.ts:12) 增加了后端地址、接口映射、SSE 分片处理、错误处理注释。  
2. 在 [chatStore.ts](f:/ideaProjects/rin1-ai-agent/fronted/src/stores/chatStore.ts:65) 增加了 `mode/chatId/分片拼接/sync兜底/会话切换` 注释。  
3. 在 [.env.example](f:/ideaProjects/rin1-ai-agent/fronted/.env.example:1) 增加了后端地址用途注释。  

另外我已验证过：注释更新后 `npm run build` 通过。