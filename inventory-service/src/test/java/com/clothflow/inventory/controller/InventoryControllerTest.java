package com.clothflow.inventory.controller;

import com.clothflow.inventory.config.JacksonConfig;
import com.clothflow.inventory.dto.CreateInventoryRequest;
import com.clothflow.inventory.dto.InventoryResponse;
import com.clothflow.inventory.dto.ReservationRequest;
import com.clothflow.inventory.exception.GlobalExceptionHandler;
import com.clothflow.inventory.exception.InsufficientStockException;
import com.clothflow.inventory.exception.InventoryNotFoundException;
import com.clothflow.inventory.service.InventoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(InventoryController.class)
@Import({
        GlobalExceptionHandler.class,
        JacksonConfig.class
})
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InventoryService inventoryService;

    @Test
    void shouldCreateInventory() throws Exception {

        UUID productId = UUID.randomUUID();

        CreateInventoryRequest request =
                new CreateInventoryRequest(
                        productId,
                        100L
                );

        InventoryResponse response =
                new InventoryResponse(
                        UUID.randomUUID(),
                        productId,
                        100L,
                        0L,
                        100L,
                        0L,
                        LocalDateTime.now(),
                        LocalDateTime.now()
                );

        when(inventoryService.createInventory(any(CreateInventoryRequest.class)))
                .thenReturn(response);

        mockMvc.perform(
                        post("/api/v1/inventory")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value(productId.toString()))
                .andExpect(jsonPath("$.quantity").value(100))
                .andExpect(jsonPath("$.reservedQuantity").value(0))
                .andExpect(jsonPath("$.availableQuantity").value(100));
    }

    @Test
    void shouldGetInventory() throws Exception {

        UUID productId = UUID.randomUUID();

        InventoryResponse response =
                new InventoryResponse(
                        UUID.randomUUID(),
                        productId,
                        100L,
                        20L,
                        80L,
                        0L,
                        LocalDateTime.now(),
                        LocalDateTime.now()
                );

        when(inventoryService.getInventory(productId))
                .thenReturn(response);

        mockMvc.perform(
                        get("/api/v1/inventory/{productId}", productId)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(productId.toString()))
                .andExpect(jsonPath("$.quantity").value(100))
                .andExpect(jsonPath("$.reservedQuantity").value(20))
                .andExpect(jsonPath("$.availableQuantity").value(80));
    }

    @Test
    void shouldReserveStock() throws Exception {

        UUID productId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();

        ReservationRequest request =
                new ReservationRequest(
                        reservationId,
                        20L
                );

        InventoryResponse response =
                new InventoryResponse(
                        UUID.randomUUID(),
                        productId,
                        100L,
                        20L,
                        80L,
                        1L,
                        LocalDateTime.now(),
                        LocalDateTime.now()
                );

        when(
                inventoryService.reserveStock(
                        eq(productId),
                        any(ReservationRequest.class)
                )
        ).thenReturn(response);

        mockMvc.perform(
                        post("/api/v1/inventory/{productId}/reservations", productId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservedQuantity").value(20))
                .andExpect(jsonPath("$.availableQuantity").value(80));
    }

    @Test
    void shouldReturn404WhenInventoryDoesNotExist() throws Exception {

        UUID productId = UUID.randomUUID();

        when(inventoryService.getInventory(productId))
                .thenThrow(
                        new InventoryNotFoundException(productId)
                );

        mockMvc.perform(
                        get("/api/v1/inventory/{productId}", productId)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error")
                        .value("INVENTORY_NOT_FOUND"))
                .andExpect(jsonPath("$.message")
                        .value("Inventory not found for product: " + productId))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/inventory/" + productId));
    }

    @Test
    void shouldReturn409WhenStockIsInsufficient() throws Exception {

        UUID productId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();

        ReservationRequest request =
                new ReservationRequest(
                        reservationId,
                        20L
                );

        when(
                inventoryService.reserveStock(
                        eq(productId),
                        any(ReservationRequest.class)
                )
        ).thenThrow(
                new InsufficientStockException(20L, 5L)
        );

        mockMvc.perform(
                        post("/api/v1/inventory/{productId}/reservations", productId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error")
                        .value("INSUFFICIENT_STOCK"))
                .andExpect(jsonPath("$.message")
                        .value("Insufficient stock. Requested: 20, Available: 5"))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/inventory/" + productId + "/reservations"));
    }

    @Test
    void shouldReturn400WhenRequestValidationFails() throws Exception {

        UUID productId = UUID.randomUUID();

        CreateInventoryRequest request =
                new CreateInventoryRequest(
                        productId,
                        -10L
                );

        mockMvc.perform(
                        post("/api/v1/inventory")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error")
                        .value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message")
                        .value("quantity: Quantity cannot be negative"))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/inventory"));
    }
}