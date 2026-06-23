package com.forum.service;

import com.forum.dto.CategoryDTO;
import com.forum.dto.ResponseDTO;
import com.forum.entity.Category;
import com.forum.mapper.CategoryMapper;
import com.forum.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final com.forum.repository.ThreadRepository threadRepository;

    private void enrichCategoryDTOs(List<CategoryDTO> dtos) {
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
        CategoryDTO savedDto = categoryMapper.toDTO(categoryRepository.save(category));
        enrichCategoryDTOs(java.util.Collections.singletonList(savedDto));
        return ResponseDTO.success(savedDto);
    }

    public ResponseDTO<CategoryDTO> updateCategory(Long id, CategoryDTO categoryDTO) {
        return categoryRepository.findById(id).map(category -> {
            category.setName(categoryDTO.getName());
            category.setDescription(categoryDTO.getDescription());
            category.setPositionOrder(categoryDTO.getPositionOrder());
            category.setActive(categoryDTO.isActive());
            
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
}
