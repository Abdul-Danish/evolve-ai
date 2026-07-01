package com.evolve.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.evolve.model.UploadObjectDto;

@Service
public class UploadService {

	private final KafkaTemplate<String, Object> kafkaTemplate;
	
	@Value("${topic.vector.upload}")
	private String vectorUploadTopic;
	
	public UploadService(KafkaTemplate<String, Object> kafkaTemplate) {
		this.kafkaTemplate = kafkaTemplate;
	}
	
	public Map<String, Object> uploadVector(UploadObjectDto uploadObjectDto) {
		Map<String, Object> response = new HashMap<>();
 		kafkaTemplate.send(vectorUploadTopic, uploadObjectDto);
		
 		response.put("status", "success");
		response.put("file", uploadObjectDto.getModuleName());
		return null;
	}
	
}
