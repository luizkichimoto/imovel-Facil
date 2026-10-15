package br.com.fiap.imovel_Facil.property;

import br.com.fiap.imovel_Facil.error.PropertyNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PropertyTransactions {
    private static final Logger LOGGER = LoggerFactory.getLogger(PropertyTransactions.class);
    private final PropertyRepository repository;

    public PropertyTransactions(PropertyRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public PropertyResponse findById(long id) {
        LOGGER.info("Consultando imóvel {} no banco de dados", id);
        return PropertyResponse.from(requireProperty(id));
    }

    @Transactional
    public PropertyResponse create(PropertyRequest request) {
        Property property = new Property();
        request.applyTo(property);
        return PropertyResponse.from(repository.save(property));
    }

    @Transactional
    public PropertyResponse update(long id, PropertyRequest request) {
        Property property = requireProperty(id);
        request.applyTo(property);
        repository.flush();
        return PropertyResponse.from(property);
    }

    @Transactional
    public void delete(long id) {
        repository.delete(requireProperty(id));
        repository.flush();
    }

    private Property requireProperty(long id) {
        return repository.findById(id).orElseThrow(PropertyNotFoundException::new);
    }
}
