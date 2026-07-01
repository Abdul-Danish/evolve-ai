package com.evolve.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelConfig {

	@Value("${spring.ai.ollama.base-url}")
	private String ollmaBaseUrl;

	@Bean
	public ChatModel llamaChatModel() {
		return OllamaChatModel.builder().ollamaApi(OllamaApi.builder().baseUrl(ollmaBaseUrl).build())
				.options(OllamaChatOptions.builder().model("llama3.2").build()).build();
	}

	@Bean
	public ChatModel qwenChatModel() {
		return OllamaChatModel.builder().ollamaApi(OllamaApi.builder().baseUrl(ollmaBaseUrl).build())
				.options(OllamaChatOptions.builder().model("qwen2.5:1.5b").keepAlive("-1m").build()).build();
	}

	@Bean
	public ChatClient llamaChatClient(@Qualifier("llamaChatModel") ChatModel llamaChatModel, ChatMemory chatMemory) {
		return ChatClient.builder(llamaChatModel).defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
				.build();
	}

	@Bean
	public ChatClient qwenChatClient(@Qualifier("qwenChatModel") ChatModel qwenChatModel, ChatMemory chatMemory) {
		return ChatClient.create(qwenChatModel);
	}

	@Bean
	public ChatMemory chatMemory() {
		return MessageWindowChatMemory.builder().chatMemoryRepository(new InMemoryChatMemoryRepository())
				.maxMessages(20).build();
	}
}
