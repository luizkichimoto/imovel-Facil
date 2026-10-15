package br.com.fiap.imovel_Facil.property;

import java.math.BigDecimal;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class PropertySpecifications {
    private PropertySpecifications() {
    }

    public static Specification<Property> titleContains(String title) {
        String pattern = "%" + title.toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        return (root, query, builder) -> builder.like(builder.lower(root.get("title")), pattern, '\\');
    }

    public static Specification<Property> cityEquals(String city) {
        return (root, query, builder) -> builder.equal(builder.lower(root.get("city")), city.toLowerCase(Locale.ROOT));
    }

    public static Specification<Property> priceAtLeast(BigDecimal price) {
        return (root, query, builder) -> builder.greaterThanOrEqualTo(root.get("price"), price);
    }

    public static Specification<Property> priceAtMost(BigDecimal price) {
        return (root, query, builder) -> builder.lessThanOrEqualTo(root.get("price"), price);
    }

    public static Specification<Property> bedroomsAtLeast(Integer bedrooms) {
        return (root, query, builder) -> builder.greaterThanOrEqualTo(root.get("bedrooms"), bedrooms);
    }

    public static Specification<Property> areaAtLeast(BigDecimal area) {
        return (root, query, builder) -> builder.greaterThanOrEqualTo(root.get("area"), area);
    }

    public static Specification<Property> matching(PropertySearchQuery filters) {
        Specification<Property> specification = (root, query, builder) -> builder.conjunction();
        if (filters.title() != null) specification = specification.and(titleContains(filters.title()));
        if (filters.city() != null) specification = specification.and(cityEquals(filters.city()));
        if (filters.minPrice() != null) specification = specification.and(priceAtLeast(filters.minPrice()));
        if (filters.maxPrice() != null) specification = specification.and(priceAtMost(filters.maxPrice()));
        if (filters.minBedrooms() != null) specification = specification.and(bedroomsAtLeast(filters.minBedrooms()));
        if (filters.minArea() != null) specification = specification.and(areaAtLeast(filters.minArea()));
        return specification;
    }
}
