package com.evolve.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.evolve.prompt.SystemPrompts;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Service
@Slf4j
public class ChatService {

	private final VectorStore vectorStore;
	private final ChatClient llamaChatClient;
	private final EvaluationService evaluationService;

	public ChatService(VectorStore vectorStore, @Qualifier("llamaChatClient") ChatClient llamaChatClient,
			EvaluationService evaluationService) {
		this.vectorStore = vectorStore;
		this.llamaChatClient = llamaChatClient;
		this.evaluationService = evaluationService;
	}

	public Flux<String> chat(String message) {
		log.info("Processing Message: {}", message);
		PromptTemplate promptTemplate = new PromptTemplate(SystemPrompts.SYSTEM_PROMPT);
		List<String> similarDocs = findSimilarDocuments(message);
		Map<String, Object> promptParameters = new HashMap<>();
		promptParameters.put("input", message);
		promptParameters.put("documents", similarDocs);
		Prompt prompt = promptTemplate.create(promptParameters);

		if (similarDocs.size() == 0) {
			return Flux.just("I couldn't find enough relevant information.");
		}
//		EvaluationResponse evaluationResponse = evaluationService.evaluateRequest(message, similarDocs);
//		if (!evaluationResponse.isShouldAnswer() || evaluationResponse.getConfidence() < 0.5) {
//			return Flux.just("I couldn't find enough relevant information.");	
//		}

		return llamaChatClient.prompt(prompt).advisors(a -> a.param(ChatMemory.CONVERSATION_ID, "user-123")).stream()
				.content();
	}

	private List<String> findSimilarDocuments(String message) {
		List<Document> similarDocuments = vectorStore
				.similaritySearch(SearchRequest.builder().query(message).topK(1).similarityThreshold(0.6).build());
		log.info("similar documents found: {}", similarDocuments.size());
		return similarDocuments.stream().map(doc -> doc.getFormattedContent()).toList();
	}

}
