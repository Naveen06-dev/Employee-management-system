package com.ems.modules.recruitment.controller;

import com.ems.common.dto.ApiResponse;
import com.ems.exception.BadRequestException;
import com.ems.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/recruitment")
public class FileUploadController {

    private final Path fileStorageLocation;

    public FileUploadController(@Value("${app.upload.dir}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException ex) {
            log.error("Could not create the directory where the uploaded files will be stored.", ex);
            throw new RuntimeException("Could not create upload directory", ex);
        }
    }

    @PostMapping("/upload-resume")
    public ResponseEntity<ApiResponse<String>> uploadResume(@RequestParam("file") MultipartFile file) {
        log.info("API request to upload file: {}", file.getOriginalFilename());

        if (file.isEmpty()) {
            throw new BadRequestException("Failed to store empty file.");
        }

        // Validate file type (only allow PDF or DOCX)
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("application/pdf") && 
                !contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))) {
            throw new BadRequestException("Invalid file type. Only PDF and DOCX files are allowed.");
        }

        try {
            String originalFileName = StringUtilsClean(Objects.requireNonNull(file.getOriginalFilename()));
            String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            String fileName = UUID.randomUUID().toString() + fileExtension;

            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Return relative file endpoint path
            String fileDownloadUri = "/api/v1/recruitment/resumes/" + fileName;
            log.info("File uploaded successfully. Storage URI: {}", fileDownloadUri);
            return ResponseEntity.ok(ApiResponse.success("Resume uploaded successfully", fileDownloadUri));

        } catch (IOException ex) {
            log.error("Could not store file. Please try again!", ex);
            throw new BadRequestException("Could not store file: " + ex.getMessage());
        }
    }

    @GetMapping("/resumes/{fileName:.+}")
    public ResponseEntity<Resource> downloadResume(@PathVariable String fileName) {
        log.info("API request to download/view file: {}", fileName);
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                // Determine content type dynamically
                String contentType = "application/octet-stream";
                if (fileName.endsWith(".pdf")) {
                    contentType = "application/pdf";
                } else if (fileName.endsWith(".docx")) {
                    contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                }

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                throw new ResourceNotFoundException("File not found: " + fileName);
            }
        } catch (MalformedURLException ex) {
            log.error("File malformed: {}", fileName, ex);
            throw new ResourceNotFoundException("File not found: " + fileName);
        }
    }

    private String StringUtilsClean(String name) {
        return Paths.get(name).getFileName().toString();
    }
}
