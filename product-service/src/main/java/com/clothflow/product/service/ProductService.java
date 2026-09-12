package com.clothflow.product.service;

import com.clothflow.product.dto.*;
import com.clothflow.product.entity.Product;
import com.clothflow.product.exception.DuplicateSkuException;
import com.clothflow.product.exception.ProductNotFoundException;
import com.clothflow.product.repository.ProductRepository;
import com.clothflow.product.repository.ProductSpecifications;
import jakarta.persistence.OptimisticLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class ProductService {

    private static final Logger log =
            LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductResponse createProduct(CreateProductRequest request) {

        if (productRepository.findBySku(request.sku()).isPresent()) {
            throw new DuplicateSkuException(request.sku());
        }

        Product product = new Product();

        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setSku(request.sku());

        Product savedProduct = productRepository.save(product);

        log.info(
                "Product created successfully: productId={}, sku={}",
                savedProduct.getId(),
                savedProduct.getSku()
        );

        return toResponse(savedProduct);
    }

    public ProductResponse updateProduct(
            UUID id,
            UpdateProductRequest request) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ProductNotFoundException(id));

        if (!product.getVersion().equals(request.version())) {
            throw new OptimisticLockException(
                    "Product was modified by another request"
            );
        }

        productRepository.findBySku(request.sku())
                .filter(existingProduct ->
                        !existingProduct.getId().equals(id))
                .ifPresent(existingProduct -> {
                    throw new DuplicateSkuException(request.sku());
                });

        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setSku(request.sku());

        Product updatedProduct =
                productRepository.saveAndFlush(product);

        log.info(
                "Product updated successfully: productId={}, version={}",
                updatedProduct.getId(),
                updatedProduct.getVersion()
        );

        return toResponse(updatedProduct);
    }

    public void deleteProduct(UUID id, Long version) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ProductNotFoundException(id));

        if (product.isDeleted()) {
            throw new ProductNotFoundException(id);
        }

        if (!product.getVersion().equals(version)) {
            throw new OptimisticLockException(
                    "Product was modified by another request"
            );
        }

        product.setDeleted(true);

        log.info(
                "Product deleted successfully: productId={}, version={}",
                id,
                product.getVersion()
        );

        productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> searchProducts(
            ProductSearchRequest request,
            Pageable pageable) {

        Specification<Product> specification =
                ProductSpecifications.isNotDeleted();

        if (request.name() != null && !request.name().isBlank()) {
            specification = specification.and(
                    ProductSpecifications.nameContains(request.name())
            );
        }

        if (request.minPrice() != null) {
            specification = specification.and(
                    ProductSpecifications.priceGreaterThanOrEqualTo(
                            request.minPrice()
                    )
            );
        }

        if (request.maxPrice() != null) {
            specification = specification.and(
                    ProductSpecifications.priceLessThanOrEqualTo(
                            request.maxPrice()
                    )
            );
        }

        Page<Product> productPage =
                productRepository.findAll(specification, pageable);

        return new PageResponse<>(
                productPage.getContent()
                        .stream()
                        .map(this::toResponse)
                        .toList(),
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalElements(),
                productPage.getTotalPages(),
                productPage.isFirst(),
                productPage.isLast()
        );
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(UUID id) {

        Product product = productRepository.findById(id)
                .filter(existingProduct -> !existingProduct.isDeleted())
                .orElseThrow(() ->
                        new ProductNotFoundException(id));

        return toResponse(product);
    }

    private ProductResponse toResponse(Product product) {

        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getSku(),
                product.getVersion(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}