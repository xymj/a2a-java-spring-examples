/*
 * Copyright 2024-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package a2a.example.agents.flight;

import static org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor.TOP_K;
import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

import java.time.LocalDate;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;

/**
 * * @author Christian Tzolov
 * 模拟的是一个航空公司 Funnair 的客户支持助手，具备：
 * 自然语言交互（ChatClient）
 * 记忆能力（ChatMemory）
 * 知识检索（RAG via VectorStore）
 * 函数调用（Function Calling）
 */
@Service
public class FlightAgent {

	private final ChatClient chatClient;

    public FlightAgent(ChatClient.Builder modelBuilder, VectorStore vectorStore,
        ChatMemory chatMemory, @Value("classpath:flight.txt") Resource promptResource) {

		// @formatter:off
		this.chatClient = modelBuilder
				.defaultSystem(promptResource)
				// 插件组合
				.defaultAdvisors(
						PromptChatMemoryAdvisor.builder(chatMemory).build(), // Chat Memory
						// new VectorStoreChatMemoryAdvisor(vectorStore)),
					
						new QuestionAnswerAdvisor(vectorStore), // RAG
						// new QuestionAnswerAdvisor(vectorStore, SearchRequest.defaults()
						// 	.withFilterExpression("'documentType' == 'terms-of-service' && region in ['EU', 'US']")),

						// logger
						new SimpleLoggerAdvisor()
				).defaultToolNames(
						"getBookingDetails",
						"changeBooking",
						"cancelBooking"
				).build();
		// @formatter:on
	}

	public Flux<String> chat(String chatId, String userMessageContent) {

		return this.chatClient.prompt()
				.system(s -> s.param("current_date", LocalDate.now().toString()))
				.user(userMessageContent)
				.advisors(
						// 设置advisor参数，
						// 记忆使用chatId，
						// 拉取最近的100条记录
						a -> a.param(CONVERSATION_ID, chatId).param(TOP_K, 100))
				.stream()
				.content();
	}

}
