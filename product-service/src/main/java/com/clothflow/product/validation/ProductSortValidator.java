package com.clothflow.product.validation;

import org.springframework.data.domain.Sort;

import java.util.Set;

public final class ProductSortValidator {

    private static final Set<String> ALLOWED_FIELDS = Set.of(
            "createdAt",
            "updatedAt",
            "name",
            "price",
            "stockQuantity"
    );

    private ProductSortValidator() {
    }

    public static void validate(Sort sort) {

        sort.forEach(order -> {

            if (!ALLOWED_FIELDS.contains(order.getProperty())) {
                throw new IllegalArgumentException(
                        "Sorting by '" + order.getProperty()
                                + "' is not supported"
                );
            }
        });
    }
}