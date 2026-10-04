package com.forum.service;

import com.forum.dto.MenuDTO;
import com.forum.dto.ResponseDTO;
import com.forum.entity.Menu;
import com.forum.mapper.MenuMapper;
import com.forum.repository.MenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository menuRepository;
    private final MenuMapper menuMapper;

    public ResponseDTO<List<MenuDTO>> getAllMenus() {
        return ResponseDTO.success(menuMapper.toDTOList(menuRepository.findAllByOrderByPositionOrderAsc()));
    }

    public ResponseDTO<MenuDTO> getMenuById(Long id) {
        return menuRepository.findById(id)
                .map(menu -> ResponseDTO.success(menuMapper.toDTO(menu)))
                .orElseThrow(() -> new RuntimeException("Menu not found"));
    }

    public ResponseDTO<MenuDTO> createMenu(MenuDTO menuDTO) {
        Menu menu = menuMapper.toEntity(menuDTO);
        return ResponseDTO.success(menuMapper.toDTO(menuRepository.save(menu)));
    }

    public ResponseDTO<MenuDTO> updateMenu(Long id, MenuDTO menuDTO) {
        return menuRepository.findById(id).map(menu -> {
            menu.setTitle(menuDTO.getTitle());
            menu.setUrl(menuDTO.getUrl());
            menu.setPositionOrder(menuDTO.getPositionOrder());
            menu.setActive(menuDTO.isActive());
            return ResponseDTO.success(menuMapper.toDTO(menuRepository.save(menu)));
        }).orElseThrow(() -> new RuntimeException("Menu not found"));
    }

    public ResponseDTO<Void> deleteMenu(Long id) {
        menuRepository.deleteById(id);
        return ResponseDTO.success(null);
    }
}
