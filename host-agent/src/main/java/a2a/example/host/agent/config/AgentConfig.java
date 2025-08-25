package a2a.example.host.agent.config;


import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AgentConfig {

  /**
   * 存储多轮对话历史（基于内存） 实现上下文感知的连续对话
   *
   * @return
   */
  @Bean
  public ChatMemory chatMemory() {
    return MessageWindowChatMemory.builder().build();
  }

  /**
   * 提供可自定义的HTTP客户端（用于调用外部API）
   *
   * @return
   */
  @Bean
  @ConditionalOnMissingBean
  public RestClient.Builder restClientBuilder() {
    return RestClient.builder();
  }

}
