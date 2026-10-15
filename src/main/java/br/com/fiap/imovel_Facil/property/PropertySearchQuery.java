package br.com.fiap.imovel_Facil.property;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record PropertySearchQuery(
        @Parameter(description = "Trecho literal do título, ignorando maiúsculas e minúsculas")
        @Size(max = 150) String title,
        @Parameter(description = "Cidade exata, ignorando maiúsculas e minúsculas")
        @Size(max = 100) String city,
        @Parameter(description = "Preço mínimo inclusivo, em reais")
        @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal minPrice,
        @Parameter(description = "Preço máximo inclusivo, em reais")
        @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal maxPrice,
        @Parameter(description = "Quantidade mínima de quartos") @Min(0) @Max(100) Integer minBedrooms,
        @Parameter(description = "Área mínima inclusiva, em metros quadrados")
        @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal minArea
) {
    public PropertySearchQuery {
        title = normalize(title);
        city = normalize(city);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
