package com.evolve.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import io.minio.errors.MinioException;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MinioService {

	private final MinioClient minioClient;

	@Value("${minio.bucket.name}")
	private String bucketName;

	public MinioService(MinioClient minioClient) {
		this.minioClient = minioClient;
	}

	public String uploadObject(String fileName, byte[] content, String filePath) throws MinioException {
		String contentType = null;
		try {
			log.info("File Name: {}", fileName);
			String fileExtension = fileName.split("\\.")[1];
			if ("pdf".toString().equals(fileExtension)) {
				contentType = MediaType.APPLICATION_PDF.toString();
			} else {
				contentType = MediaType.APPLICATION_JSON.toString();
			}
			log.info("content type: {}", contentType);

			filePath = !filePath.endsWith(File.separator) ? filePath + File.separator : filePath;
			filePath = filePath.concat(fileName);
			log.info("Constructed File Path: {}", filePath);
			PutObjectArgs putObjectArgs = PutObjectArgs.builder().bucket(bucketName).object(filePath)
					.contentType(contentType).stream(new ByteArrayInputStream(content), content.length - 1, -1).build();
			ObjectWriteResponse response = minioClient.putObject(putObjectArgs);
			log.info("Upload Response: {}", response);
			return filePath;
		} catch (Exception e) {
			log.error("Exception While Uploading Object: ", e);
			throw new MinioException("Failed to Upload Object: ");
		}
	}

	public String getPresignedUrl(String filePath) {
		GetPresignedObjectUrlArgs getPresignedObjectUrlArgs = GetPresignedObjectUrlArgs.builder().bucket(bucketName)
				.object(filePath).method(Method.GET).build();
		try {
			return minioClient.getPresignedObjectUrl(getPresignedObjectUrlArgs);
		} catch (Exception e) {
			throw new RuntimeException("Exception Occured While Getting Pre-Signed Url for the Object: ", e);
		}
	}

	public String getFile(String filePath) {
		log.info("File Path is: {}", filePath);
		GetObjectArgs getObjectArgs = GetObjectArgs.builder().bucket(bucketName).object(filePath).build();
		try {
			GetObjectResponse object = minioClient.getObject(getObjectArgs);
			log.info("Object Retrieved: {}", object.object());
			byte[] objectBytes = object.readAllBytes();
			return Base64.getEncoder().encodeToString(objectBytes);
		} catch (Exception e) {
			throw new RuntimeException("Exception Occured While Retrieving File: ", e);
		}
	}

}
