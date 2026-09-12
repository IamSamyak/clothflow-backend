package com.clothflow.inventory.controller;

import com.clothflow.inventory.InventoryIntegrationTest;
import com.clothflow.inventory.entity.Inventory;
import com.clothflow.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class InventoryControllerIntegrationTest
        extends InventoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Test
    void shouldGetInventory() throws Exception {

        UUID productId = UUID.randomUUID();

        Inventory inventory = new Inventory();

        inventory.setProductId(productId);
        inventory.setQuantity(10L);
        inventory.setReservedQuantity(2L);

        inventoryRepository.save(inventory);

        mockMvc.perform(
                        get("/api/v1/inventory/{productId}", productId)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId")
                        .value(productId.toString()))
                .andExpect(jsonPath("$.quantity")
                        .value(10))
                .andExpect(jsonPath("$.reservedQuantity")
                        .value(2))
                .andExpect(jsonPath("$.availableQuantity")
                        .value(8));
    }
}