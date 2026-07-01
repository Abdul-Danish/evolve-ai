package com.evolve.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.evolve.model.EvaluationResponse;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EvaluationService {

	private final ChatClient qwenChatClient;
	private final RestClient restClient;

	@Value("classpath:/prompt-templates/evaluation_prompt.st")
	private Resource evaluation_prompt;

	public EvaluationService(@Qualifier("qwenChatClient") ChatClient qwenChatClient) {
		this.qwenChatClient = qwenChatClient;
		this.restClient = RestClient.create("http://localhost:11434");
	}

	public EvaluationResponse evaluateRequest(String message, List<String> similarDocs) {
		log.info("Evaluating Message: {}", message);
		PromptTemplate promptTemplate = new PromptTemplate(evaluation_prompt);
		Map<String, Object> promptParameters = new HashMap<>();
		promptParameters.put("question", message);
		promptParameters.put("documents", similarDocs);
		Prompt prompt = promptTemplate.create(promptParameters);

		EvaluationResponse response = qwenChatClient.prompt(prompt).call().entity(EvaluationResponse.class);
		log.info("evaluation response: {}", response);
		return response;
	}

	@PreDestroy
	public void unloadQwen() {
		restClient.post().uri("/api/generate").body(Map.of("model", "qwen2.5:1.5b", "keep_alive", 0)).retrieve()
				.toBodilessEntity();
	}

}
