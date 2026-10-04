package com.forum.lab.service;

import com.forum.lab.document.LabDatasetRow;
import com.forum.lab.document.LabProduct;
import com.forum.lab.dto.DatasetRowDTO;
import com.forum.lab.repository.LabDatasetRowRepository;
import com.forum.lab.repository.LabProductRepository;
import com.forum.lab.utils.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatasetService {

    private final LabDatasetRowRepository datasetRowRepository;
    private final LabProductRepository productRepository;

    public Page<DatasetRowDTO> getRows(String publicId, Pageable pageable) {
        return datasetRowRepository.findByProductIdOrderByOrderIndexAsc(publicId, pageable).map(this::toDTO);
    }

    public List<DatasetRowDTO> getAllRows(String publicId) {
        return datasetRowRepository.findByProductIdOrderByOrderIndexAsc(publicId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public DatasetRowDTO addRow(String publicId, String ownerUsername, Map<String, Object> data) {
        LabProduct product = productRepository.findByPublicId(publicId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm: " + publicId));

        if (!product.getOwnerUsername().equals(ownerUsername)) {
            throw new AccessDeniedException("Bạn không có quyền thêm dữ liệu vào sản phẩm này");
        }

        long count = datasetRowRepository.countByProductId(publicId);
        if (count >= Constants.DEFAULT_MAX_ROWS_PER_PRODUCT) {
            throw new IllegalArgumentException("Sản phẩm đã đạt giới hạn tối đa " + Constants.DEFAULT_MAX_ROWS_PER_PRODUCT + " dòng dữ liệu");
        }

        LabDatasetRow row = LabDatasetRow.builder()
                .productId(publicId)
                .orderIndex((int) count)
                .data(data)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return toDTO(datasetRowRepository.save(row));
    }

    public DatasetRowDTO updateRow(String publicId, String rowId, String ownerUsername, Map<String, Object> data) {
        LabProduct product = productRepository.findByPublicId(publicId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm: " + publicId));

        if (!product.getOwnerUsername().equals(ownerUsername)) {
            throw new AccessDeniedException("Bạn không có quyền sửa dữ liệu sản phẩm này");
        }

        LabDatasetRow row = datasetRowRepository.findById(rowId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy dòng dữ liệu: " + rowId));

        row.setData(data);
        row.setUpdatedAt(Instant.now());

        return toDTO(datasetRowRepository.save(row));
    }

    public void deleteRow(String publicId, String rowId, String ownerUsername) {
        LabProduct product = productRepository.findByPublicId(publicId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm: " + publicId));

        if (!product.getOwnerUsername().equals(ownerUsername)) {
            throw new AccessDeniedException("Bạn không có quyền xoá dữ liệu sản phẩm này");
        }

        datasetRowRepository.deleteById(rowId);
    }

    public List<DatasetRowDTO> addRowsBatch(String publicId, String ownerUsername, List<Map<String, Object>> rowsData) {
        LabProduct product = productRepository.findByPublicId(publicId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm: " + publicId));

        if (!product.getOwnerUsername().equals(ownerUsername)) {
            throw new AccessDeniedException("Bạn không có quyền thêm dữ liệu vào sản phẩm này");
        }

        long currentCount = datasetRowRepository.countByProductId(publicId);
        if (currentCount + rowsData.size() > Constants.DEFAULT_MAX_ROWS_PER_PRODUCT) {
            throw new IllegalArgumentException("Vượt quá giới hạn tối đa " + Constants.DEFAULT_MAX_ROWS_PER_PRODUCT + " dòng dữ liệu");
        }

        List<LabDatasetRow> rowsToSave = new java.util.ArrayList<>();
        int seq = (int) currentCount;
        Instant now = Instant.now();
        for (Map<String, Object> data : rowsData) {
            rowsToSave.add(LabDatasetRow.builder()
                    .productId(publicId)
                    .orderIndex(seq++)
                    .data(data)
                    .createdAt(now)
                    .updatedAt(now)
                    .build());
        }

        return datasetRowRepository.saveAll(rowsToSave).stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<DatasetRowDTO> generateAiRows(String publicId, String ownerUsername, int count) {
        LabProduct product = productRepository.findByPublicId(publicId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm: " + publicId));

        if (!product.getOwnerUsername().equals(ownerUsername)) {
            throw new AccessDeniedException("Bạn không có quyền thao tác trên sản phẩm này");
        }

        int targetCount = Math.min(Math.max(count, 1), 10);
        long currentCount = datasetRowRepository.countByProductId(publicId);
        if (currentCount + targetCount > Constants.DEFAULT_MAX_ROWS_PER_PRODUCT) {
            throw new IllegalArgumentException("Sản phẩm đã gần chạm ngưỡng giới hạn dòng dữ liệu (" + Constants.DEFAULT_MAX_ROWS_PER_PRODUCT + ")");
        }

        List<Map<String, Object>> generatedRows = new java.util.ArrayList<>();
        String template = product.getTemplateType();

        for (int i = 1; i <= targetCount; i++) {
            Map<String, Object> row = new java.util.HashMap<>();
            long num = currentCount + i;
            if (Constants.TEMPLATE_LUCKY_WHEEL.equalsIgnoreCase(template)) {
                row.put("label", "Phần thưởng số " + num);
                row.put("color", (num % 2 == 0) ? "#3b82f6" : "#10b981");
                row.put("probability", 10);
            } else if (Constants.TEMPLATE_FLASHCARD.equalsIgnoreCase(template)) {
                row.put("front", "Thuật ngữ kiến thức #" + num);
                row.put("back", "Định nghĩa chi tiết cho thuật ngữ #" + num + " được AI sinh tự động.");
                row.put("category", "Kiến thức chung");
            } else if (Constants.TEMPLATE_MILLIONAIRE.equalsIgnoreCase(template)) {
                row.put("question", "Câu hỏi trắc nghiệm kiến thức số " + num + " do AI tạo là gì?");
                row.put("option_a", "Phương án A");
                row.put("option_b", "Phương án B");
                row.put("option_c", "Phương án C");
                row.put("option_d", "Phương án D");
                row.put("correct_answer", "A");
                row.put("explanation", "Giải thích chi tiết vì sao A là đáp án chính xác cho câu #" + num);
            } else {
                if (product.getColumns() != null) {
                    for (var col : product.getColumns()) {
                        row.put(col.getCode(), col.getName() + " #" + num);
                    }
                }
            }
            generatedRows.add(row);
        }

        return addRowsBatch(publicId, ownerUsername, generatedRows);
    }

    private DatasetRowDTO toDTO(LabDatasetRow r) {
        return DatasetRowDTO.builder()
                .id(r.getId())
                .productId(r.getProductId())
                .orderIndex(r.getOrderIndex())
                .data(r.getData())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
