package com.forum.mapper;

import com.forum.dto.MenuDTO;
import com.forum.entity.Menu;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MenuMapper {
    MenuMapper INSTANCE = Mappers.getMapper(MenuMapper.class);

    MenuDTO toDTO(Menu menu);
    
    Menu toEntity(MenuDTO menuDTO);
    
    List<MenuDTO> toDTOList(List<Menu> menus);
}
