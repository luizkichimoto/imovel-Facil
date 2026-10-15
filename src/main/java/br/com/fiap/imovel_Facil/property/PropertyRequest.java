package br.com.fiap.imovel_Facil.property;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Schema(name = "PropertyRequest")
public record PropertyRequest(
        @NotBlank @Size(max = 150) String title,
        @Size(max = 2000) String description,
        @NotNull PropertyType type,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Size(max = 100) String neighborhood,
        @NotNull @DecimalMin("0.01") @Digits(integer = 12, fraction = 2)
        @Schema(description = "Aluguel mensal em reais", example = "3200.00") BigDecimal price,
        @NotNull @Min(0) @Max(100) Integer bedrooms,
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2)
        @Schema(description = "Área em metros quadrados", example = "68.5") BigDecimal area
) {
    public PropertyRequest {
        title = title == null ? null : title.strip();
        description = description == null ? null : description.strip();
        city = city == null ? null : city.strip();
        neighborhood = neighborhood == null ? null : neighborhood.strip();
    }

    public void applyTo(Property property) {
        property.setTitle(title);
        property.setDescription(description);
        property.setType(type);
        property.setCity(city);
        property.setNeighborhood(neighborhood);
        property.setPrice(price);
        property.setBedrooms(bedrooms);
        property.setArea(area);
    }
}
