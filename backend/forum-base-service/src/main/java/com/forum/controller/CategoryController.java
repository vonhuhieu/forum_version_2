package com.forum.controller;

import com.forum.dto.CategoryDTO;
import com.forum.dto.ResponseDTO;
import com.forum.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<ResponseDTO<List<CategoryDTO>>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    @PostMapping
    public ResponseEntity<ResponseDTO<CategoryDTO>> createCategory(@RequestBody CategoryDTO categoryDTO) {
        return ResponseEntity.ok(categoryService.createCategory(categoryDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseDTO<CategoryDTO>> updateCategory(@PathVariable Long id, @RequestBody CategoryDTO categoryDTO) {
        try {
            return ResponseEntity.ok(categoryService.updateCategory(id, categoryDTO));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDTO<Void>> deleteCategory(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(categoryService.deleteCategory(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/by-group/{groupId}")
    public ResponseEntity<ResponseDTO<Void>> deleteCategoriesByGroupId(@PathVariable Long groupId) {
        try {
            return ResponseEntity.ok(categoryService.deleteCategoriesByGroupId(groupId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseDTO.fail(null, e.getMessage()));
        }
    }

    @GetMapping("/{id}/labels")
    public ResponseEntity<ResponseDTO<List<com.forum.dto.LabelDTO>>> getCategoryLabels(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getCategoryLabels(id));
    }

    @GetMapping("/{id}/label-config")
    public ResponseEntity<ResponseDTO<com.forum.dto.CategoryLabelConfigDTO>> getCategoryLabelConfig(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getCategoryLabelConfig(id));
    }

    @PutMapping("/{id}/label-config")
    public ResponseEntity<ResponseDTO<Void>> updateCategoryLabelConfig(@PathVariable Long id, @RequestBody com.forum.dto.CategoryLabelConfigDTO dto) {
        return ResponseEntity.ok(categoryService.updateCategoryLabelConfig(id, dto));
    }
}
