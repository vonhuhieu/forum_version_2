package com.forum.service;

import com.forum.dto.PageResponseDTO;
import com.forum.dto.ReportDTO;
import com.forum.dto.ReportGroupDTO;
import com.forum.dto.ResponseDTO;
import com.forum.entity.Post;
import com.forum.entity.Report;
import com.forum.entity.Thread;
import com.forum.entity.User;
import com.forum.repository.PostRepository;
import com.forum.repository.ReportRepository;
import com.forum.repository.ThreadRepository;
import com.forum.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ThreadRepository threadRepository;
    private final PostRepository postRepository;
    private final ThreadService threadService;
    private final PostService postService;

    public ResponseDTO<ReportDTO> createReport(ReportDTO dto, String reporterUsername) {
        User reporter = userRepository.findByUsername(reporterUsername)
                .orElseThrow(() -> new RuntimeException("Reporter not found"));

        if (dto.getTargetType() == null || dto.getTargetId() == null) {
            throw new RuntimeException("Target type and ID are required");
        }

        String targetType = dto.getTargetType().toUpperCase();
        if (!"THREAD".equals(targetType) && !"POST".equals(targetType)) {
            throw new RuntimeException("Invalid target type");
        }

        // Validate target existence and ownership
        if ("THREAD".equals(targetType)) {
            Thread thread = threadRepository.findById(dto.getTargetId())
                    .orElseThrow(() -> new RuntimeException("Thread not found"));
            if (thread.getAuthor() != null && thread.getAuthor().getId().equals(reporter.getId())) {
                throw new RuntimeException("You cannot report your own thread");
            }
        } else {
            Post post = postRepository.findById(dto.getTargetId())
                    .orElseThrow(() -> new RuntimeException("Post not found"));
            if (post.getAuthor() != null && post.getAuthor().getId().equals(reporter.getId())) {
                throw new RuntimeException("You cannot report your own post");
            }
        }

        Report report = new Report();
        report.setReason(dto.getReason());
        report.setTargetType(targetType);
        report.setTargetId(dto.getTargetId());
        report.setReporter(reporter);
        report.setStatus("PENDING");

        Report saved = reportRepository.save(report);
        return ResponseDTO.success(convertToDTO(saved));
    }

    @Transactional(readOnly = true)
    public ResponseDTO<PageResponseDTO<ReportDTO>> getReportsPaged(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Report> reportPage;

        if (status != null && !status.trim().isEmpty()) {
            reportPage = reportRepository.findByStatusOrderByCreatedAtDesc(status.trim().toUpperCase(), pageable);
        } else {
            reportPage = reportRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        List<ReportDTO> dtos = reportPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        PageResponseDTO<ReportDTO> pageResponse = new PageResponseDTO<>(
                dtos,
                reportPage.getTotalPages(),
                reportPage.getTotalElements(),
                reportPage.getNumber(),
                reportPage.getSize()
        );

        return ResponseDTO.success(pageResponse);
    }

    @Transactional(readOnly = true)
    public ResponseDTO<PageResponseDTO<ReportGroupDTO>> getGroupedReportsPaged(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        String searchStatus = (status != null && !status.trim().isEmpty()) ? status.trim().toUpperCase() : null;
        
        Page<ReportGroupDTO> groupPage = reportRepository.findGroupedReports(searchStatus, pageable);
        
        // Populate additional details
        for (ReportGroupDTO dto : groupPage.getContent()) {
            if ("THREAD".equals(dto.getTargetType())) {
                Optional<Thread> threadOpt = threadRepository.findById(dto.getTargetId());
                if (threadOpt.isPresent()) {
                    Thread t = threadOpt.get();
                    dto.setThreadId(t.getId());
                    dto.setTargetAuthorUsername(t.getAuthor() != null ? t.getAuthor().getUsername() : "Ẩn danh");
                    dto.setTargetContentSnippet("Chủ đề: " + t.getTitle());
                } else {
                    dto.setTargetAuthorUsername("N/A (Đã xóa)");
                    dto.setTargetContentSnippet("Nội dung đã bị xóa trước đó");
                }
            } else {
                Optional<Post> postOpt = postRepository.findById(dto.getTargetId());
                if (postOpt.isPresent()) {
                    Post p = postOpt.get();
                    dto.setThreadId(p.getThread() != null ? p.getThread().getId() : null);
                    dto.setTargetAuthorUsername(p.getAuthor() != null ? p.getAuthor().getUsername() : "Ẩn danh");

                    String cleanContent = p.getContent() != null ? p.getContent().replaceAll("<[^>]*>", "") : "";
                    if (cleanContent.length() > 80) {
                        cleanContent = cleanContent.substring(0, 80) + "...";
                    }
                    dto.setTargetContentSnippet("Bình luận: " + cleanContent);
                } else {
                    dto.setTargetAuthorUsername("N/A (Đã xóa)");
                    dto.setTargetContentSnippet("Nội dung đã bị xóa trước đó");
                }
            }
        }

        PageResponseDTO<ReportGroupDTO> pageResponse = new PageResponseDTO<>(
                groupPage.getContent(),
                groupPage.getTotalPages(),
                groupPage.getTotalElements(),
                groupPage.getNumber(),
                groupPage.getSize()
        );

        return ResponseDTO.success(pageResponse);
      }

      @Transactional(readOnly = true)
      public ResponseDTO<PageResponseDTO<ReportDTO>> getReportsByTargetPaged(String targetType, Long targetId, String status, int page, int size) {
          Pageable pageable = PageRequest.of(page, size);
          Page<Report> reportPage;
          
          String searchStatus = (status != null && !status.trim().isEmpty()) ? status.trim().toUpperCase() : null;
          if (searchStatus != null) {
              reportPage = reportRepository.findByTargetTypeAndTargetIdAndStatusOrderByCreatedAtDesc(
                      targetType.toUpperCase(), targetId, searchStatus, pageable);
          } else {
              reportPage = reportRepository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc(
                      targetType.toUpperCase(), targetId, pageable);
          }

          List<ReportDTO> dtos = reportPage.getContent().stream()
                  .map(this::convertToDTO)
                  .collect(Collectors.toList());

          PageResponseDTO<ReportDTO> pageResponse = new PageResponseDTO<>(
                  dtos,
                  reportPage.getTotalPages(),
                  reportPage.getTotalElements(),
                  reportPage.getNumber(),
                  reportPage.getSize()
          );

          return ResponseDTO.success(pageResponse);
      }

    public ResponseDTO<Void> resolveReport(Long reportId, String status, boolean deleteContent, String adminUsername) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        User admin = userRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new RuntimeException("Admin user not found"));

        String nextStatus = status.trim().toUpperCase();
        if (!"RESOLVED".equals(nextStatus) && !"REJECTED".equals(nextStatus)) {
            throw new RuntimeException("Invalid status update");
        }

        report.setStatus(nextStatus);
        report.setResolvedBy(admin);
        report.setResolvedAt(LocalDateTime.now());

        if ("RESOLVED".equals(nextStatus) && deleteContent) {
            if ("THREAD".equals(report.getTargetType())) {
                if (threadRepository.existsById(report.getTargetId())) {
                    threadService.deleteThread(report.getTargetId());
                }
            } else if ("POST".equals(report.getTargetType())) {
                if (postRepository.existsById(report.getTargetId())) {
                    postService.deletePost(report.getTargetId());
                }
            }
        }

        reportRepository.save(report);
        return ResponseDTO.success(null);
    }

    public ResponseDTO<Void> resolveReportGroup(String targetType, Long targetId, String status, boolean deleteContent, String adminUsername) {
        User admin = userRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new RuntimeException("Admin user not found"));

        String nextStatus = status.trim().toUpperCase();
        if (!"RESOLVED".equals(nextStatus) && !"REJECTED".equals(nextStatus)) {
            throw new RuntimeException("Invalid status update");
        }

        // Find all reports in PENDING status for this target
        List<Report> reports = reportRepository.findByTargetTypeAndTargetIdAndStatusOrderByCreatedAtDesc(
                targetType.toUpperCase(), targetId, "PENDING");

        for (Report report : reports) {
            report.setStatus(nextStatus);
            report.setResolvedBy(admin);
            report.setResolvedAt(LocalDateTime.now());
            reportRepository.save(report);
        }

        if ("RESOLVED".equals(nextStatus) && deleteContent) {
            if ("THREAD".equals(targetType.toUpperCase())) {
                if (threadRepository.existsById(targetId)) {
                    threadService.deleteThread(targetId);
                }
            } else if ("POST".equals(targetType.toUpperCase())) {
                if (postRepository.existsById(targetId)) {
                    postService.deletePost(targetId);
                }
            }
        }

        return ResponseDTO.success(null);
    }

    private ReportDTO convertToDTO(Report report) {
        ReportDTO dto = new ReportDTO();
        dto.setId(report.getId());
        dto.setReason(report.getReason());
        dto.setTargetType(report.getTargetType());
        dto.setTargetId(report.getTargetId());
        dto.setReporterId(report.getReporter().getId());
        dto.setReporterUsername(report.getReporter().getUsername());
        dto.setStatus(report.getStatus());
        dto.setCreatedAt(report.getCreatedAt());

        if (report.getResolvedBy() != null) {
            dto.setResolvedByUsername(report.getResolvedBy().getUsername());
            dto.setResolvedAt(report.getResolvedAt());
        }

        // Fetch details of the reported target
        if ("THREAD".equals(report.getTargetType())) {
            Optional<Thread> threadOpt = threadRepository.findById(report.getTargetId());
            if (threadOpt.isPresent()) {
                Thread t = threadOpt.get();
                dto.setThreadId(t.getId());
                dto.setTargetAuthorUsername(t.getAuthor() != null ? t.getAuthor().getUsername() : "Ẩn danh");
                dto.setTargetContentSnippet("Chủ đề: " + t.getTitle());
            } else {
                dto.setTargetAuthorUsername("N/A (Đã xóa)");
                dto.setTargetContentSnippet("Nội dung đã bị xóa trước đó");
            }
        } else {
            Optional<Post> postOpt = postRepository.findById(report.getTargetId());
            if (postOpt.isPresent()) {
                Post p = postOpt.get();
                dto.setThreadId(p.getThread() != null ? p.getThread().getId() : null);
                dto.setTargetAuthorUsername(p.getAuthor() != null ? p.getAuthor().getUsername() : "Ẩn danh");

                // Strip HTML tags for clean snippet text
                String cleanContent = p.getContent() != null ? p.getContent().replaceAll("<[^>]*>", "") : "";
                if (cleanContent.length() > 80) {
                    cleanContent = cleanContent.substring(0, 80) + "...";
                }
                dto.setTargetContentSnippet("Bình luận: " + cleanContent);
            } else {
                dto.setTargetAuthorUsername("N/A (Đã xóa)");
                dto.setTargetContentSnippet("Nội dung đã bị xóa trước đó");
            }
        }

        return dto;
    }
}
