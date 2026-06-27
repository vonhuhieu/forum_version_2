package com.forum.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Handles file uploads by sending them to Cloudinary CDN.
 *
 * Why Cloudinary instead of local disk?
 * - Hugging Face Spaces container filesystem is ephemeral:
 *   files written to local disk are lost on every restart/redeploy.
 * - Cloudinary stores files permanently and serves them via CDN.
 * - Returns absolute HTTPS URLs, so frontend displays images correctly
 *   regardless of which domain (Vercel vs HF Spaces) it is served from.
 *
 * Resource type strategy:
 * - image/* MIME types → Cloudinary resource_type "image"
 * - video/* MIME types → resource_type "video"
 * - Documents (pdf, doc, xlsx…) → resource_type "raw" with use_filename=true
 *   so the URL contains the original extension and is publicly fetchable
 *   cross-origin without auth (unlike image/upload which blocks cross-origin fetch).
 */
@Service
public class FileUploadService {

    @Value("${app.upload.provider:local}")
    private String uploadProvider;

    @Value("${app.upload.local-dir:uploads}")
    private String localDir;

    @Autowired
    private Cloudinary cloudinary;

    /**
     * Determine the Cloudinary resource_type for a given MIME type.
     * Documents must use "raw" to avoid the /image/upload/ URL which
     * Cloudinary restricts for cross-origin fetch requests.
     */
    private String resolveResourceType(String mimeType) {
        if (mimeType == null) return "raw";
        if (mimeType.startsWith("image/")) return "image";
        if (mimeType.startsWith("video/")) return "video";
        // PDF, Word, Excel, text, zip, etc. → raw
        return "raw";
    }

    /**
     * Upload a single file. Supports both Local and Cloudinary providers.
     * @return map with keys: url (absolute or relative URL), name, type
     */
    public Map<String, String> uploadFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String originalFilename = file.getOriginalFilename();
        String mimeType = file.getContentType();

        // 1. Local File Storage Provider
        if ("local".equalsIgnoreCase(uploadProvider)) {
            // Create local uploads directory if it does not exist
            java.nio.file.Path uploadPath = java.nio.file.Paths.get(localDir);
            if (!java.nio.file.Files.exists(uploadPath)) {
                java.nio.file.Files.createDirectories(uploadPath);
            }

            // Extract original file extension
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            // Generate unique, safe filename using UUID
            String uniqueFilename = java.util.UUID.randomUUID().toString() + extension;
            java.nio.file.Path targetPath = uploadPath.resolve(uniqueFilename);

            // Copy file content to destination path
            java.nio.file.Files.copy(file.getInputStream(), targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            // Return relative path matching Frontend expectations (/uploads/...)
            String fileUrl = "/uploads/" + uniqueFilename;

            Map<String, String> data = new HashMap<>();
            data.put("url", fileUrl);
            data.put("name", originalFilename);
            data.put("type", mimeType);
            return data;
        }

        // 2. Cloudinary Provider (Fallback)
        String resourceType = resolveResourceType(mimeType);

        Map<String, Object> uploadOptions = new HashMap<>();
        uploadOptions.put("folder", "forum_uploads");
        uploadOptions.put("resource_type", resourceType);

        // For raw uploads (documents): preserve the original filename and extension
        // by generating a unique public_id that explicitly ends with the file extension.
        // This ensures Cloudinary stores the file with the extension and delivery URLs do not return 404.
        if ("raw".equals(resourceType)) {
            if (originalFilename == null) {
                originalFilename = "file";
            }

            // Extract base name and extension
            String extension = "";
            int dotIndex = originalFilename.lastIndexOf('.');
            String baseName = originalFilename;
            if (dotIndex >= 0) {
                baseName = originalFilename.substring(0, dotIndex);
                extension = originalFilename.substring(dotIndex); // includes the dot (e.g., .pdf)
            }

            // Clean the base name to keep only alphanumeric, hyphens, and underscores
            String cleanBase = baseName.replaceAll("[^a-zA-Z0-9_\\-]", "_");
            cleanBase = cleanBase.replaceAll("__+", "_");
            cleanBase = cleanBase.replaceAll("^_+|_+$", "");
            if (cleanBase.isEmpty()) {
                cleanBase = "file";
            }
            if (cleanBase.length() > 80) {
                cleanBase = cleanBase.substring(0, 80);
            }

            // Generate a unique suffix to avoid collisions
            String uniqueSuffix = java.util.UUID.randomUUID().toString().substring(0, 8);
            String publicId = cleanBase + "_" + uniqueSuffix + extension;

            uploadOptions.put("public_id", publicId);
        }

        Map uploadResult = cloudinary.uploader().upload(
            file.getBytes(),
            uploadOptions
        );

        // secure_url is always HTTPS, works everywhere
        String secureUrl = (String) uploadResult.get("secure_url");

        Map<String, String> data = new HashMap<>();
        data.put("url",  secureUrl);
        data.put("name", originalFilename);
        data.put("type", mimeType);

        return data;
    }

    /**
     * Upload multiple files to Cloudinary.
     */
    public List<Map<String, String>> uploadMultipleFiles(MultipartFile[] files) throws IOException {
        if (files == null || files.length == 0) {
            return new ArrayList<>();
        }

        // Limit the thread pool size to the number of files (up to a max of 10 threads)
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(files.length, 10));
        try {
            // Initiate parallel uploads using CompletableFutures running on the executor
            List<CompletableFuture<Map<String, String>>> futures = Stream.of(files)
                .map(file -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return uploadFile(file);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to upload file: " + file.getOriginalFilename(), e);
                    }
                }, executor))
                .collect(Collectors.toList());

            // Wait for all upload tasks to complete
            CompletableFuture<Void> allOf = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
            );

            allOf.join();

            // Collect results, filtering out any nulls
            return futures.stream()
                .map(CompletableFuture::join)
                .filter(data -> data != null)
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new IOException("Error during concurrent multiple file upload: " + e.getMessage(), e);
        } finally {
            // Always shut down the executor pool to prevent resource leaks
            executor.shutdown();
        }
    }
}
