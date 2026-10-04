package com.forum.lab.service;

import com.forum.lab.document.LabChatSession;
import com.forum.lab.document.LabDatasetRow;
import com.forum.lab.document.LabProduct;
import com.forum.lab.dto.ProductDTO;
import com.forum.lab.dto.ProductUpdateDTO;
import com.forum.lab.repository.LabChatSessionRepository;
import com.forum.lab.repository.LabDatasetRowRepository;
import com.forum.lab.repository.LabMessageRepository;
import com.forum.lab.repository.LabProductRepository;
import com.forum.lab.template.TemplateDefinition;
import com.forum.lab.template.TemplateRegistry;
import com.forum.lab.utils.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final LabProductRepository productRepository;
    private final LabDatasetRowRepository datasetRowRepository;
    private final LabChatSessionRepository sessionRepository;
    private final LabMessageRepository messageRepository;
    private final TemplateRegistry templateRegistry;

    public ProductDTO createProductFromTemplate(String sessionId, String ownerUsername, String templateType, String customTitle) {
        TemplateDefinition template = templateRegistry.getTemplate(templateType)
                .orElseThrow(() -> new IllegalArgumentException("Khuôn mẫu không tồn tại: " + templateType));

        String publicId = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        boolean isGuest = !StringUtils.hasText(ownerUsername) || ownerUsername.startsWith(Constants.GUEST_USERNAME_PREFIX);

        Instant expiresAt = isGuest ? Instant.now().plus(Duration.ofHours(24)) : Instant.now().plus(Duration.ofDays(7));
        String title = StringUtils.hasText(customTitle) ? customTitle : template.getName();

        LabProduct product = LabProduct.builder()
                .publicId(publicId)
                .sessionId(sessionId)
                .ownerUsername(ownerUsername)
                .title(title)
                .templateType(templateType)
                .spec(template.getDefaultSpec())
                .columns(template.getDefaultColumns())
                .isPublic(true)
                .isSaved(false)
                .version(1)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .expiresAt(expiresAt)
                .build();

        LabProduct savedProduct = productRepository.save(product);

        // Seed mock rows
        if (template.getMockRows() != null) {
            int order = 0;
            for (Map<String, Object> rowData : template.getMockRows()) {
                LabDatasetRow row = LabDatasetRow.builder()
                        .productId(publicId)
                        .orderIndex(order++)
                        .data(rowData)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build();
                datasetRowRepository.save(row);
            }
        }

        // Link with chat session
        if (StringUtils.hasText(sessionId)) {
            sessionRepository.findBySessionId(sessionId).ifPresent(session -> {
                session.setProductId(publicId);
                session.setUpdatedAt(Instant.now());
                sessionRepository.save(session);
            });
        }

        log.info("Khởi tạo sản phẩm Lab thành công: {} (template: {}, publicId: {})", title, templateType, publicId);
        return toDTO(savedProduct);
    }

    public ProductDTO saveProduct(String publicId, String ownerUsername) {
        LabProduct product = productRepository.findByPublicId(publicId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm với mã: " + publicId));

        if (!product.getOwnerUsername().equals(ownerUsername)) {
            throw new AccessDeniedException("Bạn không phải chủ sở hữu của sản phẩm này");
        }

        long count = productRepository.countByOwnerUsernameAndIsSavedTrue(ownerUsername);
        if (count >= Constants.DEFAULT_MAX_PRODUCTS_PER_USER) {
            throw new IllegalArgumentException("Bạn đã đạt giới hạn tối đa " + Constants.DEFAULT_MAX_PRODUCTS_PER_USER + " sản phẩm được lưu.");
        }

        product.setSaved(true);
        product.setExpiresAt(null); // Gỡ TTL để lưu vĩnh viễn
        product.setUpdatedAt(Instant.now());

        return toDTO(productRepository.save(product));
    }

    public ProductDTO updateProduct(String publicId, String ownerUsername, ProductUpdateDTO dto) {
        LabProduct product = productRepository.findByPublicId(publicId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm: " + publicId));

        if (!product.getOwnerUsername().equals(ownerUsername)) {
            throw new AccessDeniedException("Bạn không có quyền chỉnh sửa sản phẩm này");
        }

        if (StringUtils.hasText(dto.getTitle())) {
            product.setTitle(dto.getTitle());
        }
        if (dto.getSpec() != null) {
            product.setSpec(dto.getSpec());
        }
        if (dto.getColumns() != null && !dto.getColumns().isEmpty()) {
            product.setColumns(dto.getColumns());
        }
        if (dto.getIsPublic() != null) {
            product.setPublic(dto.getIsPublic());
        }

        product.setVersion(product.getVersion() + 1);
        product.setUpdatedAt(Instant.now());

        return toDTO(productRepository.save(product));
    }

    public void deleteProduct(String publicId, String ownerUsername, boolean isAdmin) {
        LabProduct product = productRepository.findByPublicId(publicId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm: " + publicId));

        if (!isAdmin && !product.getOwnerUsername().equals(ownerUsername)) {
            throw new AccessDeniedException("Bạn không có quyền xoá sản phẩm này");
        }

        // 1. Xoá dòng dữ liệu
        datasetRowRepository.deleteByProductId(publicId);

        // 2. Xoá sản phẩm
        productRepository.deleteByPublicId(publicId);

        // 3. Xoá luôn phiên chat tương ứng (nhất quán theo Q8)
        if (StringUtils.hasText(product.getSessionId())) {
            messageRepository.deleteBySessionId(product.getSessionId());
            sessionRepository.deleteBySessionId(product.getSessionId());
        }

        log.info("Đã xoá hoàn toàn sản phẩm {} và phiên trò chuyện {}", publicId, product.getSessionId());
    }

    public ProductDTO getProductByPublicId(String publicId) {
        LabProduct product = productRepository.findByPublicId(publicId)
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại hoặc đã hết hạn: " + publicId));

        return toDTO(product);
    }

    public Page<ProductDTO> getUserProducts(String ownerUsername, Pageable pageable) {
        return productRepository.findByOwnerUsernameAndIsSavedTrue(ownerUsername, pageable).map(this::toDTO);
    }

    private ProductDTO toDTO(LabProduct p) {
        return ProductDTO.builder()
                .publicId(p.getPublicId())
                .sessionId(p.getSessionId())
                .ownerUsername(p.getOwnerUsername())
                .title(p.getTitle())
                .templateType(p.getTemplateType())
                .spec(p.getSpec())
                .columns(p.getColumns())
                .isPublic(p.isPublic())
                .isSaved(p.isSaved())
                .version(p.getVersion())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
