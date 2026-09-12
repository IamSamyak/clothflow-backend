package com.clothflow.product.controller;

import com.clothflow.product.dto.PageResponse;
import com.clothflow.product.dto.ProductResponse;
import com.clothflow.product.dto.ProductSearchRequest;
import com.clothflow.product.dto.CreateProductRequest;
import com.clothflow.product.dto.UpdateProductRequest;
import com.clothflow.product.service.ProductService;
import com.clothflow.product.validation.ProductSortValidator;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * Create a new product.
     */
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody CreateProductRequest request) {

        ProductResponse response =
                productService.createProduct(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * Update an existing product.
     *
     * The client must provide the version it last read.
     * This protects against lost updates using optimistic locking.
     */
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductRequest request) {

        return ResponseEntity.ok(
                productService.updateProduct(id, request)
        );
    }

    /**
     * Soft-delete a product.
     *
     * Version is required so that a stale client cannot
     * accidentally delete a product that was modified meanwhile.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable UUID id,
            @RequestParam Long version) {

        productService.deleteProduct(id, version);

        return ResponseEntity.noContent().build();
    }

    /**
     * Search and paginate products.
     *
     * Supported filters:
     * - name
     * - minPrice
     * - maxPrice
     *
     * Inventory/stock filtering is intentionally removed because
     * stock is now owned by Inventory Service.
     */
    @GetMapping
    public ResponseEntity<PageResponse<ProductResponse>> getAllProducts(
            @Valid ProductSearchRequest request,
            @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable) {

        if (request.hasInvalidPriceRange()) {
            throw new IllegalArgumentException(
                    "Minimum price cannot be greater than maximum price"
            );
        }

        ProductSortValidator.validate(pageable.getSort());

        /*
         * Protect the API from excessively large page sizes.
         *
         * Example:
         * ?size=1000
         *
         * becomes:
         * size=100
         */
        Pageable safePageable = PageRequest.of(
                pageable.getPageNumber(),
                Math.min(pageable.getPageSize(), 100),
                pageable.getSort()
        );

        return ResponseEntity.ok(
                productService.searchProducts(
                        request,
                        safePageable
                )
        );
    }

    /**
     * Get a single active product by UUID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(
            @PathVariable UUID id) {

        return ResponseEntity.ok(
                productService.getProductById(id)
        );
    }
}