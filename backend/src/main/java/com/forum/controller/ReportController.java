package com.forum.controller;

import com.forum.dto.PageResponseDTO;
import com.forum.dto.ReportDTO;
import com.forum.dto.ResponseDTO;
import com.forum.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping("/api/reports")
    public ResponseEntity<ResponseDTO<ReportDTO>> createReport(@RequestBody ReportDTO dto) {
        try {
            String reporterUsername = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            return ResponseEntity.ok(reportService.createReport(dto, reporterUsername));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseDTO.fail(null, e.getMessage()));
        }
    }

    @GetMapping("/api/admin/reports")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ResponseDTO<PageResponseDTO<ReportDTO>>> getReports(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            return ResponseEntity.ok(reportService.getReportsPaged(status, page, size));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseDTO.fail(null, e.getMessage()));
        }
    }

    @PutMapping("/api/admin/reports/{id}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ResponseDTO<Void>> resolveReport(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload) {
        try {
            String status = (String) payload.get("status");
            Boolean deleteContent = (Boolean) payload.getOrDefault("deleteContent", false);
            String adminUsername = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            return ResponseEntity.ok(reportService.resolveReport(id, status, deleteContent != null && deleteContent, adminUsername));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseDTO.fail(null, e.getMessage()));
        }
    }
}
