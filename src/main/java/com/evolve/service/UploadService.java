package com.evolve.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.evolve.model.EvolveDocument;
import com.evolve.model.UploadObjectDto;
import com.evolve.repository.EvolveDocumentsRepository;

@Service
public class UploadService {

	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final EvolveDocumentsRepository documentsRepository;

	@Value("${topic.vector.upload}")
	private String vectorUploadTopic;

	public UploadService(KafkaTemplate<String, Object> kafkaTemplate, EvolveDocumentsRepository documentsRepository) {
		this.kafkaTemplate = kafkaTemplate;
		this.documentsRepository = documentsRepository;
	}

	public void uploadVector(UploadObjectDto uploadObjectDto) {
		Map<String, Object> response = new HashMap<>();
		kafkaTemplate.send(vectorUploadTopic, uploadObjectDto);
	}

	public List<EvolveDocument> findAll() {
		return documentsRepository.findAll();
	}

}
