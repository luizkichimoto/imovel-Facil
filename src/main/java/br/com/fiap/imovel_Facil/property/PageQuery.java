package br.com.fiap.imovel_Facil.property;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record PageQuery(
        @Parameter(description = "Página começando em zero", example = "0")
        @Min(0) @Max(1000000) Integer page,
        @Parameter(description = "Imóveis por página, de 1 a 100", example = "10")
        @Min(1) @Max(100) Integer size
) {
}
