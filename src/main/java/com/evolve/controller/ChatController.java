package com.evolve.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.evolve.model.EvolveDocument;
import com.evolve.model.DocumentUploadRequest;
import com.evolve.service.ChatService;
import com.evolve.service.DocumentUploadService;

import io.minio.errors.MinioException;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/evolve")
@CrossOrigin(origins = "*")
public class ChatController {

	private final ChatService chatService;
	private final DocumentUploadService uploadService;

	public ChatController(ChatService chatService, DocumentUploadService uploadService) {
		this.chatService = chatService;
		this.uploadService = uploadService;
	}

	@GetMapping(value = "/chat")
	public Flux<String> chatAssist(@RequestParam(value = "message") String message) {
		return chatService.chat(message);
	}

	@PostMapping("/upload")
	public ResponseEntity<Void> uploadObject(@RequestPart(value = "file") MultipartFile file,
			@RequestParam(value = "moduleName") String moduleName) throws IOException, MinioException {
		DocumentUploadRequest uploadDto = DocumentUploadRequest.builder().fileName(file.getOriginalFilename())
				.file(file.getBytes()).moduleName(moduleName).build();
		uploadService.uploadVector(uploadDto);
		return ResponseEntity.accepted().build();
	}

	@GetMapping("/documents")
	public ResponseEntity<List<EvolveDocument>> findAllDocuments() {
		return ResponseEntity.ok().body(uploadService.findAll());
	}

}
