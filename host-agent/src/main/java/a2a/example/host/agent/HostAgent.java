package a2a.example.host.agent;


import a2a.example.host.agent.service.RemoteAgentService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.ai.util.json.schema.JsonSchemaGenerator;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;
import reactor.core.publisher.Flux;


import java.lang.reflect.Method;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

@Service
public class HostAgent {

  private final String DEFAULT_TOOL_NAME = "sendMessage";

  private final ChatClient chatClient;
  private final RemoteAgentService remoteAgentService;

  public HostAgent(ChatClient.Builder modelBuilder, ChatMemory chatMemory,
                   @Value("classpath:routing_en.txt") Resource promptResource, RemoteAgentService remoteAgentService) {
    this.remoteAgentService = remoteAgentService;
    Method method = ReflectionUtils.findMethod(RemoteAgentService.class, DEFAULT_TOOL_NAME, String.class, String.class);
    this.chatClient = modelBuilder.defaultSystem(promptResource)
        .defaultAdvisors(
            PromptChatMemoryAdvisor.builder(chatMemory).build(),
            new SimpleLoggerAdvisor()
        ).defaultToolCallbacks(MethodToolCallback.builder()
            .toolDefinition(ToolDefinition.builder()
                .description("""
                    Remote agent call, remote agent call based on the user's inquiry questions and the agent name corresponding to the problem.
                    """
                )
                .name(DEFAULT_TOOL_NAME)
                .inputSchema(JsonSchemaGenerator.generateForMethodInput(method))
                .build())
            .toolMethod(method)
            .toolObject(remoteAgentService)
            .build()
        ).defaultToolNames(DEFAULT_TOOL_NAME)
        .build();
  }


  public String chat(String chatId, String userMessageContent) {
    return this.chatClient.prompt().system(s -> s.param("agents", remoteAgentService.getAgentsInfo()))
        .user(userMessageContent).advisors(
            a -> a.param(CONVERSATION_ID, chatId))
        .call().content();
  }
}
