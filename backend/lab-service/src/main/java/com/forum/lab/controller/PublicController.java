package com.forum.lab.controller;

import com.forum.lab.dto.DatasetRowDTO;
import com.forum.lab.dto.ProductDTO;
import com.forum.lab.dto.ResponseDTO;
import com.forum.lab.service.DatasetService;
import com.forum.lab.service.ProductService;
import com.forum.lab.template.TemplateDefinition;
import com.forum.lab.template.TemplateRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lab/public")
@RequiredArgsConstructor
public class PublicController {

    private final ProductService productService;
    private final DatasetService datasetService;
    private final TemplateRegistry templateRegistry;

    @GetMapping("/products/{publicId}")
    public ResponseEntity<ResponseDTO<ProductDTO>> getPublicProduct(@PathVariable String publicId) {
        return ResponseEntity.ok(ResponseDTO.success(productService.getProductByPublicId(publicId)));
    }

    @GetMapping("/products/{publicId}/rows")
    public ResponseEntity<ResponseDTO<List<DatasetRowDTO>>> getPublicProductRows(@PathVariable String publicId) {
        return ResponseEntity.ok(ResponseDTO.success(datasetService.getAllRows(publicId)));
    }

    @GetMapping("/templates")
    public ResponseEntity<ResponseDTO<List<TemplateDefinition>>> getTemplates() {
        return ResponseEntity.ok(ResponseDTO.success(templateRegistry.getAllTemplates()));
    }
}
