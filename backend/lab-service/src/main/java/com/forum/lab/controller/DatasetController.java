package com.forum.lab.controller;

import com.forum.lab.dto.DatasetRowDTO;
import com.forum.lab.dto.ResponseDTO;
import com.forum.lab.service.DatasetService;
import com.forum.lab.utils.Constants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/lab/products/{publicId}/rows")
@RequiredArgsConstructor
public class DatasetController {

    private final DatasetService datasetService;

    @GetMapping
    @PreAuthorize(Constants.PRE_AUTH_USER_OR_ADMIN)
    public ResponseEntity<ResponseDTO<Page<DatasetRowDTO>>> getRows(
            @PathVariable String publicId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ResponseDTO.success(datasetService.getRows(publicId, pageable)));
    }

    @PostMapping
    @PreAuthorize(Constants.PRE_AUTH_USER_OR_ADMIN)
    public ResponseEntity<ResponseDTO<DatasetRowDTO>> addRow(
            @PathVariable String publicId,
            @RequestBody Map<String, Object> data,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ResponseDTO.success(datasetService.addRow(publicId, authentication.getName(), data)));
    }

    @PutMapping("/{rowId}")
    @PreAuthorize(Constants.PRE_AUTH_USER_OR_ADMIN)
    public ResponseEntity<ResponseDTO<DatasetRowDTO>> updateRow(
            @PathVariable String publicId,
            @PathVariable String rowId,
            @RequestBody Map<String, Object> data,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ResponseDTO.success(datasetService.updateRow(publicId, rowId, authentication.getName(), data)));
    }

    @DeleteMapping("/{rowId}")
    @PreAuthorize(Constants.PRE_AUTH_USER_OR_ADMIN)
    public ResponseEntity<ResponseDTO<Void>> deleteRow(
            @PathVariable String publicId,
            @PathVariable String rowId,
            Authentication authentication
    ) {
        datasetService.deleteRow(publicId, rowId, authentication.getName());
        return ResponseEntity.ok(ResponseDTO.success(null));
    }

    @PostMapping("/batch")
    @PreAuthorize(Constants.PRE_AUTH_USER_OR_ADMIN)
    public ResponseEntity<ResponseDTO<java.util.List<DatasetRowDTO>>> addRowsBatch(
            @PathVariable String publicId,
            @RequestBody java.util.List<Map<String, Object>> rowsData,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ResponseDTO.success(datasetService.addRowsBatch(publicId, authentication.getName(), rowsData)));
    }

    @PostMapping("/ai-generate")
    @PreAuthorize(Constants.PRE_AUTH_USER_OR_ADMIN)
    public ResponseEntity<ResponseDTO<java.util.List<DatasetRowDTO>>> generateAiRows(
            @PathVariable String publicId,
            @RequestBody(required = false) Map<String, Object> body,
            Authentication authentication
    ) {
        int count = 5;
        if (body != null && body.containsKey("count")) {
            try {
                count = Integer.parseInt(body.get("count").toString());
            } catch (Exception ignored) {
            }
        }
        return ResponseEntity.ok(ResponseDTO.success(datasetService.generateAiRows(publicId, authentication.getName(), count)));
    }
}
