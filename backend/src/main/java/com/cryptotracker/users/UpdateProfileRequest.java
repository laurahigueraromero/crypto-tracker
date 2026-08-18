package com.cryptotracker.users;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "displayName is required")
        @Size(max = 100, message = "displayName must be at most 100 characters")
        String displayName,

        String avatarUrl,

        @NotBlank(message = "baseCurrency is required")
        @Pattern(regexp = "USD|EUR", message = "baseCurrency must be one of USD, EUR")
        String baseCurrency,

        String timezone
) {
}
