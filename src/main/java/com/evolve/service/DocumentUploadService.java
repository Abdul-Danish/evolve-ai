package com.evolve.service;

import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.evolve.model.DocumentStatus;
import com.evolve.model.DocumentUploadRequest;
import com.evolve.model.EvolveDocument;
import com.evolve.repository.DocumentRepository;

import io.minio.errors.MinioException;

@Service
public class DocumentUploadService {

	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final DocumentRepository documentsRepository;
	private final MinioService minioService;

	@Value("${minio.bucket.name}")
	private String bucketName;

	@Value("${minio.base.path}")
	private String minioBasePath;

	@Value("${topic.vector.upload}")
	private String vectorUploadTopic;

	public DocumentUploadService(KafkaTemplate<String, Object> kafkaTemplate, DocumentRepository documentsRepository,
			MinioService minioService) {
		this.kafkaTemplate = kafkaTemplate;
		this.documentsRepository = documentsRepository;
		this.minioService = minioService;
	}

	public void uploadVector(DocumentUploadRequest uploadRequest) throws MinioException {
		byte[] content = uploadRequest.getFile();
		String fileName = uploadRequest.getFileName();
		String moduleName = uploadRequest.getModuleName();
		String imagePath = minioBasePath + fileName;
		EvolveDocument document = EvolveDocument.builder().moduleName(moduleName).fileName(fileName)
				.fileExtension(FilenameUtils.getExtension(fileName)).fileSize((long) content.length)
				.minioImagePath(imagePath).status(DocumentStatus.PENDING).build();
		Long docId = documentsRepository.save(document).getId();

		String uploadedFilePath = minioService.uploadObject(fileName, content, minioBasePath);
		String presignedUrl = minioService.getPresignedUrl(uploadedFilePath);
		uploadRequest.setPresignedUrl(presignedUrl);
		uploadRequest.setDocId(docId);
		uploadRequest.setFile(null);

		kafkaTemplate.send(vectorUploadTopic, uploadRequest);
	}

	public List<EvolveDocument> findAll() {
		return documentsRepository.findAll();
	}

}
