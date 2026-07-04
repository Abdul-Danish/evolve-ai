package com.evolve.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentUploadRequest {

	private String fileName;
	private byte[] file;
	private String moduleName;
	private String presignedUrl;
	private Long docId;

}
