package com.cryptotracker.notes;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateNoteRequest(
        @NotBlank(message = "title is required")
        @Size(max = 100, message = "title must be at most 100 characters")
        String title,

        @NotBlank(message = "content is required")
        @Size(max = 2000, message = "content must be at most 2000 characters")
        String content,

        @NotNull(message = "type is required")
        @Pattern(
                regexp = "PREDICCION|MOTIVO_COMPRA|MOTIVO_VENTA|OBSERVACION",
                message = "type must be one of PREDICCION, MOTIVO_COMPRA, MOTIVO_VENTA, OBSERVACION"
        )
        String type,

        @NotEmpty(message = "at least one coinId is required")
        List<@NotBlank String> coinIds,

        List<@NotBlank String> tags
) {
}
