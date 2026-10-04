package com.forum.mapper;

import com.forum.dto.CategoryGroupDTO;
import com.forum.entity.CategoryGroup;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring", uses = {CategoryMapper.class})
public interface CategoryGroupMapper {
    CategoryGroupMapper INSTANCE = Mappers.getMapper(CategoryGroupMapper.class);

    CategoryGroupDTO toDTO(CategoryGroup categoryGroup);
    
    CategoryGroup toEntity(CategoryGroupDTO categoryGroupDTO);
    
    List<CategoryGroupDTO> toDTOList(List<CategoryGroup> categoryGroups);
}
