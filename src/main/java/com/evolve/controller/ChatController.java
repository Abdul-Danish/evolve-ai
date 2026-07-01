package com.evolve.controller;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.evolve.model.UploadObjectDto;
import com.evolve.service.ChatService;
import com.evolve.service.UploadService;

import io.minio.errors.MinioException;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class ChatController {

	private final ChatService chatService;
	private final UploadService uploadService;

	public ChatController(ChatService chatService, UploadService uploadService) {
		this.chatService = chatService;
		this.uploadService = uploadService;
	}

	@GetMapping(value = "/chat")
	public Flux<String> chatAssist(@RequestParam(value = "message") String message) {
		return chatService.chat(message);
	}

	@PostMapping("/upload")
	public ResponseEntity<Map<String, Object>> uploadObject(@RequestPart(value = "file") MultipartFile file,
			@RequestParam(value = "minioFilePath") String minioFilePath,
			@RequestParam(value = "moduleName") String moduleName) throws IOException, MinioException {

//	@PostMapping("/upload")
//	public ResponseEntity<Map<String, Object>> uploadObject(@RequestBody UploadObjectDto uploadDto)
//			throws IOException, MinioException {
		UploadObjectDto uploadDto = UploadObjectDto.builder().fileName(file.getOriginalFilename())
				.file(file.getBytes()).minioFilePath(minioFilePath).moduleName(moduleName).build();
		return ResponseEntity.accepted().body(uploadService.uploadVector(uploadDto));
	}

}
