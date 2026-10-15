package br.com.fiap.imovel_Facil.property;

import java.math.BigDecimal;
import org.springframework.hateoas.server.core.Relation;

@Relation(itemRelation = "property", collectionRelation = "properties")
public record PropertyResponse(
        Long id,
        String title,
        String description,
        PropertyType type,
        String city,
        String neighborhood,
        BigDecimal price,
        Integer bedrooms,
        BigDecimal area
) {
    public static PropertyResponse from(Property property) {
        return new PropertyResponse(property.getId(), property.getTitle(), property.getDescription(),
                property.getType(), property.getCity(), property.getNeighborhood(), property.getPrice(),
                property.getBedrooms(), property.getArea());
    }
}
