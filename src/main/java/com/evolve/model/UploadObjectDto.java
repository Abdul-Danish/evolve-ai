package com.evolve.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadObjectDto {

	private String fileName;
	private byte[] file;
	private String minioFilePath;
	private String moduleName;
	
}
