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
        categoryGroupRepository.deleteById(id);
        return ResponseDTO.success(null);
    }
}
