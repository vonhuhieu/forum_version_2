package com.forum.service;

import com.forum.dto.PageResponseDTO;
import com.forum.dto.ReportDTO;
import com.forum.dto.ResponseDTO;

public interface ReportService {
    ResponseDTO<ReportDTO> createReport(ReportDTO dto, String reporterUsername);
    ResponseDTO<PageResponseDTO<ReportDTO>> getReportsPaged(String status, int page, int size);
    ResponseDTO<Void> resolveReport(Long reportId, String status, boolean deleteContent, String adminUsername);
}
