package com.evolve.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.MinioException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;
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

	public InputStream downloadResource(String imagePath)
			throws InvalidKeyException, ErrorResponseException, InsufficientDataException, InternalException,
			InvalidResponseException, NoSuchAlgorithmException, ServerException, XmlParserException, IOException {
		GetObjectArgs args = GetObjectArgs.builder().bucket(bucketName).object(imagePath).build();
		return minioClient.getObject(args);
	}

}
