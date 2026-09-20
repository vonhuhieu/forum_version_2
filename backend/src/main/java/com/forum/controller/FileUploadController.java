package com.forum.controller;

import com.forum.dto.ResponseDTO;
import com.forum.service.FileUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    @Autowired
    private FileUploadService fileUploadService;

    @PostMapping
    public ResponseEntity<ResponseDTO<Map<String, String>>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            Map<String, String> data = fileUploadService.uploadFile(file);
            if (data == null) {
                return ResponseEntity.badRequest().body(ResponseDTO.fail(null, "Tệp tải lên không hợp lệ hoặc rỗng."));
            }
            return ResponseEntity.ok(ResponseDTO.success(data));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ResponseDTO.fail(null, ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body(ResponseDTO.fail(null, ex.getMessage()));
        }
    }

    @PostMapping("/multiple")
    public ResponseEntity<ResponseDTO<List<Map<String, String>>>> uploadMultipleFiles(@RequestParam("files") MultipartFile[] files) {
        try {
            List<Map<String, String>> uploadedFiles = fileUploadService.uploadMultipleFiles(files);
            return ResponseEntity.ok(ResponseDTO.success(uploadedFiles));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ResponseDTO.fail(null, ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body(ResponseDTO.fail(null, ex.getMessage()));
        }
    }
}
