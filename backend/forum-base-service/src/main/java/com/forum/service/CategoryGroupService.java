package com.forum.service;

import com.forum.dto.CategoryDTO;
import com.forum.dto.CategoryGroupDTO;
import com.forum.dto.ResponseDTO;
import com.forum.entity.CategoryGroup;
import com.forum.mapper.CategoryGroupMapper;
import com.forum.repository.CategoryGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class CategoryGroupService {

    private final CategoryGroupRepository categoryGroupRepository;
    private final CategoryGroupMapper categoryGroupMapper;
    private final CategoryService categoryService;
    private final jakarta.persistence.EntityManager entityManager;

    public ResponseDTO<List<CategoryGroupDTO>> getAllGroups() {
        List<CategoryGroupDTO> groups = categoryGroupMapper.toDTOList(categoryGroupRepository.findAllByOrderByPositionOrderAsc());
        if (groups != null) {
            List<CategoryDTO> allCategories = new ArrayList<>();
            for (CategoryGroupDTO group : groups) {
                if (group.getCategories() != null) {
                    allCategories.addAll(group.getCategories());
                }
            }
            categoryService.enrichCategoryDTOs(allCategories);
        }
        return ResponseDTO.success(groups);
    }

    public ResponseDTO<CategoryGroupDTO> getGroupById(Long id) {
        return categoryGroupRepository.findById(id)
                .map(group -> {
                    CategoryGroupDTO dto = categoryGroupMapper.toDTO(group);
                    if (dto != null && dto.getCategories() != null) {
                        categoryService.enrichCategoryDTOs(dto.getCategories());
                    }
                    return ResponseDTO.success(dto);
                })
                .orElseThrow(() -> new RuntimeException("Category Group not found"));
    }

    public ResponseDTO<CategoryGroupDTO> createGroup(CategoryGroupDTO groupDTO) {
        CategoryGroup group = categoryGroupMapper.toEntity(groupDTO);
        CategoryGroupDTO savedDto = categoryGroupMapper.toDTO(categoryGroupRepository.save(group));
        if (savedDto != null && savedDto.getCategories() != null) {
            categoryService.enrichCategoryDTOs(savedDto.getCategories());
        }
        return ResponseDTO.success(savedDto);
    }

    public ResponseDTO<CategoryGroupDTO> updateGroup(Long id, CategoryGroupDTO groupDTO) {
        return categoryGroupRepository.findById(id).map(group -> {
            group.setName(groupDTO.getName());
            group.setPositionOrder(groupDTO.getPositionOrder());
            group.setActive(groupDTO.isActive());
            CategoryGroupDTO savedDto = categoryGroupMapper.toDTO(categoryGroupRepository.save(group));
            if (savedDto != null && savedDto.getCategories() != null) {
                categoryService.enrichCategoryDTOs(savedDto.getCategories());
            }
            return ResponseDTO.success(savedDto);
        }).orElseThrow(() -> new RuntimeException("Category Group not found"));
    }

    public ResponseDTO<Void> deleteGroup(Long id) {
        categoryService.deleteCategoriesByGroupId(id);
        categoryGroupRepository.deleteById(id);
        return ResponseDTO.success(null);
    }

    public ResponseDTO<Void> deleteAllGroups() {
        entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate();
        try {
            entityManager.createNativeQuery("DELETE FROM poll_votes").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM poll_options").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM polls").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM reactions WHERE thread_id IS NOT NULL OR post_id IS NOT NULL").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM thread_subscriptions").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM notifications WHERE thread_id IS NOT NULL OR post_id IS NOT NULL").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM posts").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM threads").executeUpdate();
            entityManager.createNativeQuery("UPDATE categories SET parent_id = NULL").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM categories").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM category_groups").executeUpdate();
        } finally {
            entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();
        }

        entityManager.clear();
        com.forum.service.ThreadService.clearAllCaches();
        return ResponseDTO.success(null);
    }

    public ResponseDTO<Void> importSqlScript(org.springframework.web.multipart.MultipartFile file, boolean useDefault) {
        try {
            final org.springframework.core.io.Resource finalResource;
            if (file != null && !file.isEmpty()) {
                finalResource = new org.springframework.core.io.ByteArrayResource(file.getBytes());
            } else if (useDefault) {
                org.springframework.core.io.Resource r = new org.springframework.core.io.ClassPathResource("data/seed_default_categories.sql");
                if (!r.exists()) {
                    java.io.File fallbackFile = new java.io.File("scripts/seed_default_categories.sql");
                    if (fallbackFile.exists()) {
                        r = new org.springframework.core.io.FileSystemResource(fallbackFile);
                    } else {
                        return ResponseDTO.fail(null, "Không tìm thấy file seed_default_categories.sql");
                    }
                }
                finalResource = r;
            } else {
                return ResponseDTO.fail(null, "Vui lòng chọn file .sql hoặc chọn nạp mặc định");
            }

            org.hibernate.Session session = entityManager.unwrap(org.hibernate.Session.class);
            session.doWork(connection -> {
                org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(connection, finalResource);
            });

            entityManager.clear();
            com.forum.service.ThreadService.clearAllCaches();
            return ResponseDTO.success(null);
        } catch (Exception e) {
            return ResponseDTO.fail(null, "Lỗi thực thi SQL: " + e.getMessage());
        }
    }

    public byte[] getDefaultSqlTemplate() {
        try {
            org.springframework.core.io.Resource resource = new org.springframework.core.io.ClassPathResource("data/seed_default_categories.sql");
            if (resource.exists()) {
                try (java.io.InputStream is = resource.getInputStream()) {
                    return is.readAllBytes();
                }
            }
            java.io.File fallbackFile = new java.io.File("scripts/seed_default_categories.sql");
            if (fallbackFile.exists()) {
                return java.nio.file.Files.readAllBytes(fallbackFile.toPath());
            }
        } catch (Exception ignored) {
        }
        return "-- seed_default_categories.sql".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
}
