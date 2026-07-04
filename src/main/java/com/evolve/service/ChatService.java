package com.evolve.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.evolve.model.EvaluationResponse;
import com.evolve.prompt.PromptTemplates;

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
		PromptTemplate promptTemplate = new PromptTemplate(PromptTemplates.SYSTEM_PROMPT);
		List<Document> similarDocs = findSimilarDocuments(message);
		if (similarDocs.size() == 0) {
			return Flux.just("I couldn't find enough relevant information.");
		}
		List<String> docTexts = similarDocs.stream().map(Document::getText).collect(Collectors.toList());

		Map<String, Object> promptParameters = new HashMap<>();
		promptParameters.put("input", message);
		promptParameters.put("documents", docTexts);
		Prompt prompt = promptTemplate.create(promptParameters);

		EvaluationResponse evaluationResponse = evaluationService.evaluateRequest(message, docTexts);
		if (!evaluationResponse.isShouldAnswer() || evaluationResponse.getConfidence() < 0.5) {
			return Flux.just("I couldn't find enough relevant information.");
		}

		Flux<String> response = llamaChatClient.prompt(prompt)
				.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, "user-123")).stream().content();
		String reference = buildReferenceSection(similarDocs);
		return Objects.isNull(reference) ? response : Flux.concat(response, Flux.just(reference));
	}

	private String buildReferenceSection(List<Document> documents) {
		Optional<Document> documentOpt = documents.stream().findFirst();
		String ref = null;
		if (documentOpt.isPresent()) {
			Document document = documentOpt.get();
			Map<String, Object> metadata = document.getMetadata();
			String moduleName = metadata.get("moduleName").toString();
			String docUrl = metadata.get("documentUrl").toString();

			StringBuffer buffer = new StringBuffer("\n\nReference Documents:\n");
			buffer.append("Module: ").append(moduleName).append("\nReference URL: ").append(docUrl).append("\n");
			ref = buffer.toString();
		}

		return ref;
	}

	private List<Document> findSimilarDocuments(String message) {
		List<Document> similarDocuments = vectorStore
				.similaritySearch(SearchRequest.builder().query(message).topK(3).similarityThreshold(0.75).build());
		log.info("similar documents found: {}", similarDocuments.size());
		return similarDocuments;
	}

}
