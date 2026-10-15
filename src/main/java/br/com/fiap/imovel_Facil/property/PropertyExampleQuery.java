package br.com.fiap.imovel_Facil.property;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record PropertyExampleQuery(
        @Parameter(description = "Tipo exato do imóvel") PropertyType type,
        @Parameter(description = "Cidade exata, ignorando maiúsculas e minúsculas")
        @Size(max = 100) String city,
        @Parameter(description = "Bairro exato, ignorando maiúsculas e minúsculas")
        @Size(max = 100) String neighborhood,
        @Parameter(description = "Quantidade exata de quartos") @Min(0) @Max(100) Integer bedrooms
) {
    public PropertyExampleQuery {
        city = normalize(city);
        neighborhood = normalize(neighborhood);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
