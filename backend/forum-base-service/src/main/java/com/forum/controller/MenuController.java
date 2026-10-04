package com.forum.controller;

import com.forum.dto.MenuDTO;
import com.forum.dto.ResponseDTO;
import com.forum.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @GetMapping
    public ResponseEntity<ResponseDTO<List<MenuDTO>>> getAllMenus() {
        return ResponseEntity.ok(menuService.getAllMenus());
    }

    @PostMapping
    public ResponseEntity<ResponseDTO<MenuDTO>> createMenu(@RequestBody MenuDTO menuDTO) {
        return ResponseEntity.ok(menuService.createMenu(menuDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseDTO<MenuDTO>> updateMenu(@PathVariable Long id, @RequestBody MenuDTO menuDTO) {
        try {
            return ResponseEntity.ok(menuService.updateMenu(id, menuDTO));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDTO<Void>> deleteMenu(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(menuService.deleteMenu(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
