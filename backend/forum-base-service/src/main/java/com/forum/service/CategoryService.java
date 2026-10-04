package com.forum.service;

import com.forum.dto.CategoryDTO;
import com.forum.dto.CategoryLabelConfigDTO;
import com.forum.dto.LabelDTO;
import com.forum.dto.ResponseDTO;
import com.forum.entity.Category;
import com.forum.entity.Label;
import com.forum.mapper.CategoryMapper;
import com.forum.mapper.LabelMapper;
import com.forum.repository.CategoryRepository;
import com.forum.repository.LabelRepository;
import com.forum.utils.Constants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final LabelRepository labelRepository;
    private final LabelMapper labelMapper;
    private final com.forum.repository.ThreadRepository threadRepository;
    private final jakarta.persistence.EntityManager entityManager;

    public void enrichCategoryDTOs(List<CategoryDTO> dtos) {
        if (dtos == null || dtos.isEmpty()) return;
        List<Object[]> stats = threadRepository.getCategoryStats();
        java.util.Map<Long, Long[]> statsMap = new java.util.HashMap<>();
        for (Object[] row : stats) {
            if (row[0] != null) {
                statsMap.put((Long) row[0], new Long[]{(Long) row[1], (Long) row[2]});
            }
        }
        enrichCategoryDTOsRecursive(dtos, statsMap);
    }

    private void enrichCategoryDTOsRecursive(List<CategoryDTO> dtos, java.util.Map<Long, Long[]> statsMap) {
        for (CategoryDTO dto : dtos) {
            Long[] stat = statsMap.get(dto.getId());
            if (stat != null) {
                dto.setThreadCount(stat[0]);
                dto.setPostCount(stat[1]);
            } else {
                dto.setThreadCount(0L);
                dto.setPostCount(0L);
            }
            if (dto.getSubCategories() != null) {
                enrichCategoryDTOsRecursive(dto.getSubCategories(), statsMap);
            }
        }
    }

    public ResponseDTO<List<CategoryDTO>> getAllCategories() {
        List<CategoryDTO> dtos = categoryMapper.toDTOList(categoryRepository.findAllByOrderByPositionOrderAsc());
        enrichCategoryDTOs(dtos);
        return ResponseDTO.success(dtos);
    }

    public ResponseDTO<List<CategoryDTO>> getTopLevelCategories() {
        List<Category> all = categoryRepository.findAllByOrderByPositionOrderAsc();
        List<Category> topLevel = all.stream()
                .filter(c -> c.getParentCategory() == null)
                .collect(java.util.stream.Collectors.toList());
        List<CategoryDTO> dtos = categoryMapper.toDTOList(topLevel);
        enrichCategoryDTOs(dtos);
        return ResponseDTO.success(dtos);
    }

    public ResponseDTO<CategoryDTO> getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .map(category -> {
                    CategoryDTO dto = categoryMapper.toDTO(category);
                    enrichCategoryDTOs(java.util.Collections.singletonList(dto));
                    return ResponseDTO.success(dto);
                })
                .orElseThrow(() -> new RuntimeException("Category not found"));
    }

    public ResponseDTO<CategoryDTO> createCategory(CategoryDTO categoryDTO) {
        com.forum.entity.Category category = categoryMapper.toEntity(categoryDTO);
        if (categoryDTO.getCategoryGroupId() != null) {
            com.forum.entity.CategoryGroup group = new com.forum.entity.CategoryGroup();
            group.setId(categoryDTO.getCategoryGroupId());
            category.setCategoryGroup(group);
        }
        if (categoryDTO.getParentCategoryId() != null) {
            com.forum.entity.Category parent = new com.forum.entity.Category();
            parent.setId(categoryDTO.getParentCategoryId());
            category.setParentCategory(parent);
        }
        if (categoryDTO.getLabelMode() != null) {
            category.setLabelMode(categoryDTO.getLabelMode());
        }
        CategoryDTO savedDto = categoryMapper.toDTO(categoryRepository.save(category));
        enrichCategoryDTOs(java.util.Collections.singletonList(savedDto));
        return ResponseDTO.success(savedDto);
    }

    public ResponseDTO<CategoryDTO> updateCategory(Long id, CategoryDTO categoryDTO) {
        return categoryRepository.findById(id).map(category -> {
            category.setName(categoryDTO.getName());
            category.setDescription(categoryDTO.getDescription());
            category.setIcon(categoryDTO.getIcon());
            category.setPositionOrder(categoryDTO.getPositionOrder());
            category.setActive(categoryDTO.isActive());
            category.setOnlyAdminCanPost(categoryDTO.isOnlyAdminCanPost());
            if (categoryDTO.getLabelMode() != null) {
                category.setLabelMode(categoryDTO.getLabelMode());
            }
            
            if (categoryDTO.getCategoryGroupId() != null) {
                com.forum.entity.CategoryGroup group = new com.forum.entity.CategoryGroup();
                group.setId(categoryDTO.getCategoryGroupId());
                category.setCategoryGroup(group);
            } else {
                category.setCategoryGroup(null);
            }
            
            if (categoryDTO.getParentCategoryId() != null) {
                com.forum.entity.Category parent = new com.forum.entity.Category();
                parent.setId(categoryDTO.getParentCategoryId());
                category.setParentCategory(parent);
            } else {
                category.setParentCategory(null);
            }
            
            CategoryDTO savedDto = categoryMapper.toDTO(categoryRepository.save(category));
            enrichCategoryDTOs(java.util.Collections.singletonList(savedDto));
            return ResponseDTO.success(savedDto);
        }).orElseThrow(() -> new RuntimeException("Category not found"));
    }

    public ResponseDTO<Void> deleteCategory(Long id) {
        categoryRepository.deleteById(id);
        return ResponseDTO.success(null);
    }

    public ResponseDTO<Void> deleteCategoriesByGroupId(Long groupId) {
        entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate();
        try {
            // Delete threads and related objects of categories in this group if any exist
            entityManager.createNativeQuery(
                "DELETE FROM poll_votes WHERE poll_id IN (SELECT id FROM polls WHERE thread_id IN (SELECT id FROM threads WHERE category_id IN (SELECT id FROM categories WHERE category_group_id = :groupId)))"
            ).setParameter("groupId", groupId).executeUpdate();

            entityManager.createNativeQuery(
                "DELETE FROM poll_options WHERE poll_id IN (SELECT id FROM polls WHERE thread_id IN (SELECT id FROM threads WHERE category_id IN (SELECT id FROM categories WHERE category_group_id = :groupId)))"
            ).setParameter("groupId", groupId).executeUpdate();

            entityManager.createNativeQuery(
                "DELETE FROM polls WHERE thread_id IN (SELECT id FROM threads WHERE category_id IN (SELECT id FROM categories WHERE category_group_id = :groupId))"
            ).setParameter("groupId", groupId).executeUpdate();

            entityManager.createNativeQuery(
                "DELETE FROM reactions WHERE thread_id IN (SELECT id FROM threads WHERE category_id IN (SELECT id FROM categories WHERE category_group_id = :groupId)) " +
                "OR post_id IN (SELECT id FROM posts WHERE thread_id IN (SELECT id FROM threads WHERE category_id IN (SELECT id FROM categories WHERE category_group_id = :groupId)))"
            ).setParameter("groupId", groupId).executeUpdate();

            entityManager.createNativeQuery(
                "DELETE FROM thread_subscriptions WHERE thread_id IN (SELECT id FROM threads WHERE category_id IN (SELECT id FROM categories WHERE category_group_id = :groupId))"
            ).setParameter("groupId", groupId).executeUpdate();

            entityManager.createNativeQuery(
                "DELETE FROM notifications WHERE thread_id IN (SELECT id FROM threads WHERE category_id IN (SELECT id FROM categories WHERE category_group_id = :groupId)) " +
                "OR post_id IN (SELECT id FROM posts WHERE thread_id IN (SELECT id FROM threads WHERE category_id IN (SELECT id FROM categories WHERE category_group_id = :groupId)))"
            ).setParameter("groupId", groupId).executeUpdate();

            entityManager.createNativeQuery(
                "DELETE FROM posts WHERE thread_id IN (SELECT id FROM threads WHERE category_id IN (SELECT id FROM categories WHERE category_group_id = :groupId))"
            ).setParameter("groupId", groupId).executeUpdate();

            entityManager.createNativeQuery(
                "DELETE FROM threads WHERE category_id IN (SELECT id FROM categories WHERE category_group_id = :groupId)"
            ).setParameter("groupId", groupId).executeUpdate();

            entityManager.createNativeQuery(
                "UPDATE categories SET parent_id = NULL WHERE category_group_id = :groupId"
            ).setParameter("groupId", groupId).executeUpdate();

            entityManager.createNativeQuery(
                "DELETE FROM categories WHERE category_group_id = :groupId"
            ).setParameter("groupId", groupId).executeUpdate();
        } finally {
            entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();
        }

        entityManager.clear();
        com.forum.service.ThreadService.clearAllCaches();
        return ResponseDTO.success(null);
    }

    public ResponseDTO<CategoryLabelConfigDTO> getCategoryLabelConfig(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        CategoryLabelConfigDTO dto = new CategoryLabelConfigDTO();
        dto.setLabelMode(category.getLabelMode() != null ? category.getLabelMode() : Constants.CATEGORY_LABEL_MODE_ALL);
        List<Long> ids = category.getLabels() != null
                ? category.getLabels().stream().map(Label::getId).collect(Collectors.toList())
                : new ArrayList<>();
        dto.setSelectedLabelIds(ids);
        return ResponseDTO.success(dto);
    }

    public ResponseDTO<Void> updateCategoryLabelConfig(Long categoryId, CategoryLabelConfigDTO dto) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        String mode = dto.getLabelMode();
        if (mode == null || mode.trim().isEmpty()) {
            mode = Constants.CATEGORY_LABEL_MODE_ALL;
        }
        category.setLabelMode(mode);
        if (category.getLabels() == null) {
            category.setLabels(new HashSet<>());
        } else {
            category.getLabels().clear();
        }
        if (Constants.CATEGORY_LABEL_MODE_CUSTOM.equalsIgnoreCase(mode) && dto.getSelectedLabelIds() != null && !dto.getSelectedLabelIds().isEmpty()) {
            List<Label> labels = labelRepository.findAllById(dto.getSelectedLabelIds());
            category.getLabels().addAll(labels);
        }
        categoryRepository.save(category);
        return ResponseDTO.success(null);
    }

    public ResponseDTO<List<LabelDTO>> getCategoryLabels(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        String mode = category.getLabelMode();
        if (mode == null) mode = Constants.CATEGORY_LABEL_MODE_ALL;

        if (Constants.CATEGORY_LABEL_MODE_NONE.equalsIgnoreCase(mode)) {
            return ResponseDTO.success(Collections.emptyList());
        }
        if (Constants.CATEGORY_LABEL_MODE_CUSTOM.equalsIgnoreCase(mode)) {
            List<LabelDTO> dtos = category.getLabels() != null
                    ? category.getLabels().stream().map(labelMapper::toDTO).collect(Collectors.toList())
                    : Collections.emptyList();
            return ResponseDTO.success(dtos);
        }
        // ALL: Cho phép tất cả các nhãn
        return ResponseDTO.success(labelMapper.toDTOList(labelRepository.findAllByOrderByIdDesc()));
    }
}

