package in.kcmart.backend.catalog.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateInventoryRequest(
    @NotNull @Min(0) Integer onlineStock,
    @NotNull @Min(0) Integer storeStock,
    @Min(0) Integer expectedOnlineStock,
    @Min(0) Integer expectedStoreStock
) {}