package a2a.example.agents.multimodal;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.chat.MessageFormat;
import com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants;

import reactor.core.publisher.Flux;

@Component
public class MultimodalAgent {

  private static final String DEFAULT_MODEL = "qwen-vl-max-latest";
  private final ChatClient chatClient;

  public MultimodalAgent(ChatClient.Builder builder, ChatMemory chatMemory) {
    this.chatClient =
        builder.defaultSystem("""
            你是一个专业图片分析师，你能够根据图片URL，获取图片详情，分析图片内容，给出图片的描述信息。
            不要回答与图片无关的内容。
            无图片URL时，与用户交流提示提供需分析的图片URL。
            """
        ).defaultAdvisors(
            PromptChatMemoryAdvisor.builder(chatMemory).build(), new SimpleLoggerAdvisor()
        ).build();
  }

  public Flux<ChatResponse> streamImage(String chatId, String userMessageContent) {
    Map<String, Object> metadata = Map.of(DashScopeApiConstants.MESSAGE_FORMAT, MessageFormat.IMAGE);
    UserMessage message =
        UserMessage.builder().text(userMessageContent).metadata(metadata).build();

    // 返回Flux<ChatResponse>而不是收集为List并阻塞
    return chatClient
        .prompt(new Prompt(message,
            DashScopeChatOptions.builder().withModel(DEFAULT_MODEL).withMultiModel(true)
                .build()))
        .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, chatId)
            .param(VectorStoreChatMemoryAdvisor.TOP_K, 100))
        .stream().chatResponse(); // 移除.collectList().block()，直接返回流
  }
}