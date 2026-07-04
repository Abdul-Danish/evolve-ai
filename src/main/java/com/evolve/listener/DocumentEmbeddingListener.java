package com.evolve.listener;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import com.evolve.model.DocumentStatus;
import com.evolve.model.DocumentUploadRequest;
import com.evolve.model.EvolveDocument;
import com.evolve.repository.DocumentRepository;
import com.evolve.service.MinioService;

import io.minio.errors.MinioException;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DocumentEmbeddingListener {

	private final VectorStore vectorStore;
	private final MinioService minioService;
	private final DocumentRepository documentsRepository;

	@Value("${minio.bucket.name}")
	private String bucketName;

	@Value("${minio.base.path}")
	private String minioBasePath;

	public DocumentEmbeddingListener(VectorStore vectorStore, MinioService minioService,
			DocumentRepository documentsRepository) {
		this.vectorStore = vectorStore;
		this.minioService = minioService;
		this.documentsRepository = documentsRepository;
	}

	@KafkaListener(topics = "${topic.vector.upload}", groupId = "${spring.kafka.consumer.group-id}_vector", containerFactory = "kafkaListenerContainerFactory")
	public void uploadVector(ConsumerRecord<String, DocumentUploadRequest> consumerRecord,
			Acknowledgment acknowledgment) throws MinioException, IOException {
		acknowledgment.acknowledge();
		DocumentUploadRequest uploadRequest = consumerRecord.value();
		String fileName = uploadRequest.getFileName();
		String moduleName = uploadRequest.getModuleName();
		String imagePath = minioBasePath + fileName;
		String presignedUrl = uploadRequest.getPresignedUrl();
		EvolveDocument document = null;
		try {
			document = documentsRepository.findById(uploadRequest.getDocId()).get();
			InputStream resourceContent = minioService.downloadResource(imagePath);

			document.setStatus(DocumentStatus.PROCESSING);
			documentsRepository.save(document);
			uploadVector(resourceContent, presignedUrl, moduleName);

			document.setStatus(DocumentStatus.COMPLETED);
			documentsRepository.save(document);
		} catch (Exception e) {
			log.error("document upload failed ", e);
			document.setStatus(DocumentStatus.FAILED);
			document.setErrorMessage(e.getMessage());
			documentsRepository.save(document);
			throw new RuntimeException(e);
		}
	}

	public void uploadVector(InputStream content, String presignedUrl, String moduleName)
			throws InterruptedException, IOException {
		log.info("Loading Vector Documents into the Database");
		logMemory("Start Upload");
		PdfDocumentReaderConfig readerConfig = PdfDocumentReaderConfig
				.builder().withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
						.withNumberOfBottomTextLinesToDelete(0).withNumberOfTopPagesToSkipBeforeDelete(0).build())
				.withPagesPerDocument(1).build();

		PagePdfDocumentReader pageReader = new PagePdfDocumentReader(new ByteArrayResource(content.readAllBytes()),
				readerConfig);
		logMemory("After PagePdfDocumentReader");

		List<Document> documents = pageReader.get();
		logMemory("After Reading PDF");
		documents.forEach(document -> {
			document.getMetadata().put("moduleName", moduleName);
			document.getMetadata().put("documentUrl", presignedUrl);
		});

		TokenTextSplitter textSplitter = TokenTextSplitter.builder().build();
		List<Document> chunks = textSplitter.apply(documents);
		logMemory("After Chunking");
		log.info("Text splitting completed. Chunks count: {}", chunks.size());

		long start = System.currentTimeMillis();
		int batchSize = 20;
		for (int i = 0; i < chunks.size(); i += batchSize) {
			int end = Math.min(i + batchSize, chunks.size());
			vectorStore.accept(chunks.subList(i, end));
			log.info("Processed {} / {}", end, chunks.size());
		}
		logMemory("After Vector Upload");
		logResponse(start);
		log.info("Vector Documents for [{}] module Loaded Successfully", moduleName);
	}

	private void logResponse(long start) {
		long durationMs = System.currentTimeMillis() - start;
		long minutes = durationMs / 60000;
		long seconds = (durationMs % 60000) / 1000;
		log.info("Vector store insert completed in {} min {} sec", minutes, seconds);
	}

	private void logMemory(String stage) {
		long free = Runtime.getRuntime().freeMemory() / 1048576;
		long total = Runtime.getRuntime().totalMemory() / 1048576;
		long max = Runtime.getRuntime().maxMemory() / 1048576;
		long used = total - free;

		log.info("[{}] Used: {} MB | Free: {} MB | Total: {} MB | Max: {} MB", stage, used, free, total, max);
	}
}
