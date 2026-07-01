package com.evolve.listener;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.evolve.model.DocumentStatus;
import com.evolve.model.EvolveDocument;
import com.evolve.model.UploadObjectDto;
import com.evolve.repository.EvolveDocumentsRepository;
import com.evolve.service.MinioService;

import io.minio.errors.MinioException;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class VectorUploadListener {

	private final VectorStore vectorStore;
	private final MinioService minioService;
	private final EvolveDocumentsRepository documentsRepository;

	@Value("${minio.bucket.name}")
	private String bucketName;

	public VectorUploadListener(VectorStore vectorStore, MinioService minioService,
			EvolveDocumentsRepository documentsRepository) {
		this.vectorStore = vectorStore;
		this.minioService = minioService;
		this.documentsRepository = documentsRepository;
	}

	@KafkaListener(topics = "${topic.vector.upload}", groupId = "${spring.kafka.consumer.group-id}_vector")
	public void uploadVector(ConsumerRecord<String, UploadObjectDto> consumerRecord)
			throws MinioException, IOException {
		UploadObjectDto uploadObject = consumerRecord.value();
		byte[] content = uploadObject.getFile();
		String fileName = uploadObject.getFileName();
		String filePath = uploadObject.getMinioFilePath();
		String moduleName = uploadObject.getModuleName();
		EvolveDocument document = EvolveDocument.builder().moduleName(moduleName).fileName(fileName)
				.fileExtension(fileName.split(".")[0]).fileSize((long) content.length).minioObjectPath(filePath)
				.status(DocumentStatus.PENDING).build();
		String docId = documentsRepository.save(document).getId();
		try {

			String uploadedFilePath = minioService.uploadObject(fileName, content, filePath);
			String presignedUrl = minioService.getPresignedUrl(uploadedFilePath);

			Resource resourceContent = new ByteArrayResource(content);
			uploadVector(resourceContent, presignedUrl, moduleName);

			EvolveDocument doc = documentsRepository.findById(docId).get();
			doc.setStatus(DocumentStatus.COMPLETED);
			documentsRepository.save(doc);
		} catch (Exception e) {
			log.error("upload document failed ", e);
			EvolveDocument doc = documentsRepository.findById(docId).get();
			doc.setStatus(DocumentStatus.FAILED);
			doc.setErrorMessage(e.getMessage());
			documentsRepository.save(doc);
		}
	}

	public void uploadVector(Resource content, String presignedUrl, String moduleName) {
		log.info("Loading Vector Documents into the Database");

		PdfDocumentReaderConfig readerConfig = PdfDocumentReaderConfig
				.builder().withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
						.withNumberOfBottomTextLinesToDelete(0).withNumberOfTopPagesToSkipBeforeDelete(0).build())
				.withPagesPerDocument(1).build();

		PagePdfDocumentReader pageReader = new PagePdfDocumentReader(content, readerConfig);
		TokenTextSplitter textSplitter = TokenTextSplitter.builder().build();

		List<Document> documents = pageReader.get();
		documents.forEach(document -> {
			document.getMetadata().put("moduleName", moduleName);
			document.getMetadata().put("documentUrl", presignedUrl);
		});

		List<Document> chunks = textSplitter.apply(documents);
		log.info("Text splitting completed. Chunks count: {}", chunks.size());

		long start = System.currentTimeMillis();
		int batchSize = 100;
		for (int i = 0; i < chunks.size(); i += batchSize) {
			int end = Math.min(i + batchSize, chunks.size());
			vectorStore.accept(chunks.subList(i, end));
			log.info("Processed {} / {}", end, chunks.size());
		}
		logResponse(start);
		log.info("Vector Documents for [{}] module Loaded Successfully", moduleName);
	}

	private void logResponse(long start) {
		long durationMs = System.currentTimeMillis() - start;
		long minutes = durationMs / 60000;
		long seconds = (durationMs % 60000) / 1000;
		log.info("Vector store insert completed in {} min {} sec", minutes, seconds);
	}
}
