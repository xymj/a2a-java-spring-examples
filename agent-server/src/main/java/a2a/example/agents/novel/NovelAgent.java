package a2a.example.agents.novel;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class NovelAgent {

  private final ChatClient chatClient;

  public NovelAgent(ChatClient.Builder builder, ChatMemory chatMemory,
      @Value("classpath:novel.txt") Resource promptResource) {
    this.chatClient = builder
        .defaultSystem(promptResource)
        .defaultAdvisors(
            PromptChatMemoryAdvisor.builder(chatMemory).build(),
            new SimpleLoggerAdvisor()
        ).build();
  }


  public Flux<String> chat(String chatId, String userMessageContent) {
    return this.chatClient.prompt()
        .user(userMessageContent)
        .advisors(
            advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, chatId).param(VectorStoreChatMemoryAdvisor.TOP_K, 100)
        ).stream()
        .content().map(c -> {
            System.out.println(c);
            return c;
        });
  }

    public void chatResponse(String chatId, String userMessageContent) {
        this.chatClient.prompt()
            .user(userMessageContent)
            .advisors(
                advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, chatId).param(VectorStoreChatMemoryAdvisor.TOP_K, 100)
            ).stream().chatResponse().subscribe(c -> {
                System.out.println(c.toString());
          });
    }
}
