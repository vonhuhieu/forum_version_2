package com.forum.lab.controller;

import com.forum.lab.dto.ProductDTO;
import com.forum.lab.dto.ProductUpdateDTO;
import com.forum.lab.dto.ResponseDTO;
import com.forum.lab.service.ProductService;
import com.forum.lab.utils.Constants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lab/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/my")
    @PreAuthorize(Constants.PRE_AUTH_USER_OR_ADMIN)
    public ResponseEntity<ResponseDTO<Page<ProductDTO>>> getMyProducts(
            Authentication authentication,
            @PageableDefault(size = 15) Pageable pageable
    ) {
        return ResponseEntity.ok(ResponseDTO.success(productService.getUserProducts(authentication.getName(), pageable)));
    }

    @PostMapping("/{publicId}/save")
    @PreAuthorize(Constants.PRE_AUTH_USER_OR_ADMIN)
    public ResponseEntity<ResponseDTO<ProductDTO>> saveProduct(
            @PathVariable String publicId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ResponseDTO.success(productService.saveProduct(publicId, authentication.getName())));
    }

    @PutMapping("/{publicId}")
    @PreAuthorize(Constants.PRE_AUTH_USER_OR_ADMIN)
    public ResponseEntity<ResponseDTO<ProductDTO>> updateProduct(
            @PathVariable String publicId,
            @Valid @RequestBody ProductUpdateDTO dto,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ResponseDTO.success(productService.updateProduct(publicId, authentication.getName(), dto)));
    }

    @DeleteMapping("/{publicId}")
    @PreAuthorize(Constants.PRE_AUTH_USER_OR_ADMIN)
    public ResponseEntity<ResponseDTO<Void>> deleteProduct(
            @PathVariable String publicId,
            Authentication authentication
    ) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(Constants.ROLE_ADMIN) || a.getAuthority().equals(Constants.ROLE_SUPER_ADMIN));
        productService.deleteProduct(publicId, authentication.getName(), isAdmin);
        return ResponseEntity.ok(ResponseDTO.success(null));
    }
}
