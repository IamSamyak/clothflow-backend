package com.clothflow.inventory.controller;

import com.clothflow.inventory.dto.CreateInventoryRequest;
import com.clothflow.inventory.dto.InventoryResponse;
import com.clothflow.inventory.dto.ReservationRequest;
import com.clothflow.inventory.dto.StockAdjustmentRequest;
import com.clothflow.inventory.service.InventoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(
            InventoryService inventoryService) {

        this.inventoryService = inventoryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public InventoryResponse createInventory(
            @Valid @RequestBody CreateInventoryRequest request) {

        return inventoryService.createInventory(request);
    }

    @GetMapping("/{productId}")
    @PreAuthorize("isAuthenticated()")
    public InventoryResponse getInventory(
            @PathVariable
            @NotNull UUID productId) {

        return inventoryService.getInventory(productId);
    }

    @PostMapping("/{productId}/stock/add")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public InventoryResponse addStock(
            @PathVariable UUID productId,
            @Valid @RequestBody StockAdjustmentRequest request) {

        return inventoryService.addStock(
                productId,
                request
        );
    }

    @PostMapping("/{productId}/stock/remove")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public InventoryResponse removeStock(
            @PathVariable UUID productId,
            @Valid @RequestBody StockAdjustmentRequest request) {

        return inventoryService.removeStock(
                productId,
                request
        );
    }

    @PostMapping("/{productId}/reservations")
    @PreAuthorize(
            "hasAuthority('TOKEN_SERVICE') and " +
                    "hasAuthority('SCOPE_inventory.write')"
    )
    public InventoryResponse reserveStock(
            @PathVariable UUID productId,
            @Valid @RequestBody ReservationRequest request) {

        return inventoryService.reserveStock(
                productId,
                request
        );
    }

    @PostMapping("/{productId}/reservations/commit")
    @PreAuthorize(
            "hasAuthority('TOKEN_SERVICE') and " +
                    "hasAuthority('SCOPE_inventory.write')"
    )
    public InventoryResponse commitReservation(
            @PathVariable UUID productId,
            @Valid @RequestBody ReservationRequest request) {

        return inventoryService.commitReservation(
                productId,
                request
        );
    }

    @PostMapping("/{productId}/reservations/release")
    @PreAuthorize(
            "hasAuthority('TOKEN_SERVICE') and " +
                    "hasAuthority('SCOPE_inventory.write')"
    )
    public InventoryResponse releaseReservation(
            @PathVariable UUID productId,
            @Valid @RequestBody ReservationRequest request) {

        return inventoryService.releaseReservation(
                productId,
                request
        );
    }
}