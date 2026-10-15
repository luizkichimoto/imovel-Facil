package br.com.fiap.imovel_Facil.config;

import br.com.fiap.imovel_Facil.property.Property;
import br.com.fiap.imovel_Facil.property.PropertyRepository;
import br.com.fiap.imovel_Facil.property.PropertyRequest;
import br.com.fiap.imovel_Facil.property.PropertyType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataLoader implements CommandLineRunner {
    private static final List<Location> LOCATIONS = List.of(
            new Location("São Paulo", "Pinheiros"),
            new Location("Campinas", "Cambuí"),
            new Location("Santos", "Gonzaga"),
            new Location("Curitiba", "Batel"),
            new Location("Belo Horizonte", "Savassi"));
    private final PropertyRepository repository;

    public DataLoader(PropertyRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (repository.count() > 0) return;
        List<Property> properties = new ArrayList<>(60);
        PropertyType[] types = PropertyType.values();
        for (int number = 1; number <= 60; number++) {
            Location location = LOCATIONS.get((number - 1) % LOCATIONS.size());
            PropertyType type = types[(number - 1) % types.length];
            int bedrooms = switch (type) {
                case STUDIO, COMMERCIAL -> 0;
                case APARTMENT -> 2;
                case HOUSE -> 3;
            };
            String label = switch (type) {
                case APARTMENT -> "Apartamento";
                case HOUSE -> "Casa";
                case STUDIO -> "Studio";
                case COMMERCIAL -> "Sala comercial";
            };
            PropertyRequest request = new PropertyRequest(label + " em " + location.neighborhood() + " " + number,
                    "Imóvel demonstrativo com boa localização e acesso a serviços.", type,
                    location.city(), location.neighborhood(), BigDecimal.valueOf(1000L + number * 150L, 0),
                    bedrooms, BigDecimal.valueOf(30L + number * 3L, 0));
            if (number == 7) {
                request = new PropertyRequest("Apto com varanda", "Perto do metrô", PropertyType.APARTMENT,
                        "São Paulo", "Pinheiros", new BigDecimal("3200.00"), 2, new BigDecimal("68.5"));
            }
            Property property = new Property();
            request.applyTo(property);
            properties.add(property);
        }
        repository.saveAll(properties);
    }

    private record Location(String city, String neighborhood) {
    }
}
