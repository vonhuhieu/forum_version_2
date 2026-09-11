package com.forum.controller;

import com.forum.dto.CategoryGroupDTO;
import com.forum.dto.ResponseDTO;
import com.forum.service.CategoryGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/category-groups")
@RequiredArgsConstructor
public class CategoryGroupController {

    private final CategoryGroupService categoryGroupService;

    @GetMapping
    public ResponseEntity<ResponseDTO<List<CategoryGroupDTO>>> getAllGroups() {
        return ResponseEntity.ok(categoryGroupService.getAllGroups());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseDTO<CategoryGroupDTO>> getGroupById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryGroupService.getGroupById(id));
    }

    @PostMapping
    public ResponseEntity<ResponseDTO<CategoryGroupDTO>> createGroup(@RequestBody CategoryGroupDTO groupDTO) {
        return ResponseEntity.ok(categoryGroupService.createGroup(groupDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseDTO<CategoryGroupDTO>> updateGroup(@PathVariable Long id, @RequestBody CategoryGroupDTO groupDTO) {
        return ResponseEntity.ok(categoryGroupService.updateGroup(id, groupDTO));
    }

    @DeleteMapping("/delete-all")
    public ResponseEntity<ResponseDTO<Void>> deleteAllGroups() {
        try {
            return ResponseEntity.ok(categoryGroupService.deleteAllGroups());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseDTO.fail(null, e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDTO<Void>> deleteGroup(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(categoryGroupService.deleteGroup(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseDTO.fail(null, e.getMessage()));
        }
    }
}
