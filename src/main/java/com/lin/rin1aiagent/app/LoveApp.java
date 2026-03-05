package com.lin.rin1aiagent.app;

import com.lin.rin1aiagent.advisor.SimpleLoggerAdvisor;
import com.lin.rin1aiagent.chatmemory.FileBasedChatMemory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

@Component
@Slf4j
public class LoveApp {

    private static final String SYSTEM_PROMPT = "你是一个恋爱关系顾问。请始终使用简体中文回答，" +
            "给出务实、共情、结构清晰的建议，尽量用短句和分点。";

    private static final String MCP_TOOL_POLICY_PROMPT = "当工具可用时，针对时效性/事实性问题优先调用工具检索，" +
            "不要只依赖模型记忆。如果没有可用工具，明确说明当前无法进行实时联网搜索。";

    private final ChatClient chatClient;

    public LoveApp(ChatModel dashscopeChatModel) {
        String fileDir = System.getProperty("user.dir") + "/tmp/chat-memory";
        ChatMemory chatMemory = new FileBasedChatMemory(fileDir);

        this.chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        new SimpleLoggerAdvisor())
                .build();
    }

    public String doChat(String message, String chatId) {
        return chatClient
                .prompt()
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .content();
    }

    public record LoveReport(String title, List<String> suggestions) {
    }

    public LoveReport doChatWithReport(String message, String chatId) {
        return chatClient
                .prompt()
                .system(SYSTEM_PROMPT + " Always output a title and a suggestion list.")
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .entity(LoveReport.class);
    }

    @Resource
    private VectorStore vectorStore;

    public String doChatWithRag(String message, String chatId) {
        return chatClient
                .prompt()
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(RetrievalAugmentationAdvisor.builder()
                        .documentRetriever(VectorStoreDocumentRetriever.builder().vectorStore(vectorStore).build())
                        .build())
                .call()
                .content();
    }

    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    public String doChatWithMcp(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .system(SYSTEM_PROMPT + " " + MCP_TOOL_POLICY_PROMPT)
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(new SimpleLoggerAdvisor())
                .toolCallbacks(toolCallbackProvider)
                .call()
                .chatResponse();

        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    public Flux<String> doChatStream(String message, String chatId) {
        return chatClient
                .prompt()
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream()
                .content();
    }

    public Flux<String> doChatWithRagStream(String message, String chatId) {
        return chatClient
                .prompt()
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(RetrievalAugmentationAdvisor.builder()
                        .documentRetriever(VectorStoreDocumentRetriever.builder().vectorStore(vectorStore).build())
                        .build())
                .stream()
                .content();
    }

    public Flux<String> doChatWithMcpStream(String message, String chatId) {
        return chatClient
                .prompt()
                .system(SYSTEM_PROMPT + " " + MCP_TOOL_POLICY_PROMPT)
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(new SimpleLoggerAdvisor())
                .toolCallbacks(toolCallbackProvider)
                .stream()
                .content();
    }
}
