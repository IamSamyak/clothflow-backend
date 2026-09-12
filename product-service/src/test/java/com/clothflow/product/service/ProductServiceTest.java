package com.clothflow.product.service;

import com.clothflow.product.dto.CreateProductRequest;
import com.clothflow.product.dto.ProductResponse;
import com.clothflow.product.dto.UpdateProductRequest;
import com.clothflow.product.entity.Product;
import com.clothflow.product.exception.DuplicateSkuException;
import com.clothflow.product.exception.ProductNotFoundException;
import com.clothflow.product.repository.ProductRepository;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product product;
    private UUID productId;

    @BeforeEach
    void setUp() {

        productId = UUID.randomUUID();

        product = new Product();

        product.setId(productId);
        product.setName("Silk Saree");
        product.setDescription("Traditional silk saree");
        product.setPrice(new BigDecimal("4999.00"));
        product.setSku("SAR-001");
        product.setVersion(0L);
        product.setDeleted(false);
    }

    @Test
    void shouldCreateProduct() {

        CreateProductRequest request = new CreateProductRequest(
                "Silk Saree",
                "Traditional silk saree",
                new BigDecimal("4999.00"),
                "SAR-001"
        );

        when(productRepository.findBySku("SAR-001"))
                .thenReturn(Optional.empty());

        when(productRepository.save(any(Product.class)))
                .thenReturn(product);

        ProductResponse response =
                productService.createProduct(request);

        assertNotNull(response);

        assertEquals(productId, response.id());
        assertEquals("Silk Saree", response.name());
        assertEquals("SAR-001", response.sku());
        assertEquals(
                new BigDecimal("4999.00"),
                response.price()
        );

        verify(productRepository)
                .findBySku("SAR-001");

        verify(productRepository)
                .save(any(Product.class));
    }

    @Test
    void shouldRejectDuplicateSku() {

        CreateProductRequest request = new CreateProductRequest(
                "Another Saree",
                "Another product",
                new BigDecimal("3999.00"),
                "SAR-001"
        );

        when(productRepository.findBySku("SAR-001"))
                .thenReturn(Optional.of(product));

        assertThrows(
                DuplicateSkuException.class,
                () -> productService.createProduct(request)
        );

        verify(productRepository)
                .findBySku("SAR-001");

        verify(productRepository, never())
                .save(any(Product.class));
    }

    @Test
    void shouldGetProductById() {

        when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));

        ProductResponse response =
                productService.getProductById(productId);

        assertNotNull(response);

        assertEquals(productId, response.id());
        assertEquals("Silk Saree", response.name());
        assertEquals("SAR-001", response.sku());

        verify(productRepository)
                .findById(productId);
    }

    @Test
    void shouldThrowExceptionWhenProductDoesNotExist() {

        UUID unknownProductId = UUID.randomUUID();

        when(productRepository.findById(unknownProductId))
                .thenReturn(Optional.empty());

        assertThrows(
                ProductNotFoundException.class,
                () -> productService.getProductById(unknownProductId)
        );

        verify(productRepository)
                .findById(unknownProductId);
    }

    @Test
    void shouldUpdateProduct() {

        UpdateProductRequest request = new UpdateProductRequest(
                "Premium Silk Saree",
                "Premium traditional silk saree",
                new BigDecimal("5999.00"),
                "SAR-001",
                0L
        );

        when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));

        when(productRepository.findBySku("SAR-001"))
                .thenReturn(Optional.of(product));

        when(productRepository.saveAndFlush(product))
                .thenReturn(product);

        ProductResponse response =
                productService.updateProduct(
                        productId,
                        request
                );

        assertNotNull(response);

        assertEquals(
                "Premium Silk Saree",
                product.getName()
        );

        assertEquals(
                new BigDecimal("5999.00"),
                product.getPrice()
        );

        assertEquals(
                "SAR-001",
                product.getSku()
        );

        verify(productRepository)
                .saveAndFlush(product);
    }

    @Test
    void shouldRejectUpdateWithStaleVersion() {

        UpdateProductRequest request = new UpdateProductRequest(
                "Premium Silk Saree",
                "Premium traditional silk saree",
                new BigDecimal("5999.00"),
                "SAR-001",
                99L
        );

        when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));

        assertThrows(
                OptimisticLockException.class,
                () -> productService.updateProduct(
                        productId,
                        request
                )
        );

        verify(productRepository, never())
                .saveAndFlush(any(Product.class));
    }

    @Test
    void shouldSoftDeleteProduct() {

        when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));

        productService.deleteProduct(productId, 0L);

        assertTrue(product.isDeleted());

        verify(productRepository)
                .save(product);
    }

    @Test
    void shouldRejectDeleteWithStaleVersion() {

        when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));

        assertThrows(
                OptimisticLockException.class,
                () -> productService.deleteProduct(
                        productId,
                        99L
                )
        );

        verify(productRepository, never())
                .save(any(Product.class));
    }
}