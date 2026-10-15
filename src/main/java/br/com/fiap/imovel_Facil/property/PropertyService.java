package br.com.fiap.imovel_Facil.property;

import br.com.fiap.imovel_Facil.error.InvalidRequestException;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PropertyService {
    private static final Set<String> SORT_FIELDS = Set.of("id", "title", "type", "city", "neighborhood",
            "price", "bedrooms", "area");
    private final PropertyRepository repository;
    private final PropertyTransactions transactions;
    private final CachedPropertyService cached;
    private final ReentrantLock[] locks = new ReentrantLock[64];

    public PropertyService(PropertyRepository repository, PropertyTransactions transactions, CachedPropertyService cached) {
        this.repository = repository;
        this.transactions = transactions;
        this.cached = cached;
        for (int index = 0; index < locks.length; index++) {
            locks[index] = new ReentrantLock();
        }
    }

    @Transactional(readOnly = true)
    public Page<PropertyResponse> findAll(Pageable pageable) {
        return repository.findAll(normalize(pageable)).map(PropertyResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<PropertyResponse> findByExample(PropertyExampleQuery filters, Pageable pageable) {
        Property probe = new Property();
        probe.setType(filters.type());
        probe.setCity(filters.city());
        probe.setNeighborhood(filters.neighborhood());
        probe.setBedrooms(filters.bedrooms());
        ExampleMatcher matcher = ExampleMatcher.matchingAll()
                .withIgnorePaths("id", "version")
                .withIgnoreNullValues()
                .withIgnoreCase()
                .withStringMatcher(ExampleMatcher.StringMatcher.EXACT);
        return repository.findAll(Example.of(probe, matcher), normalize(pageable)).map(PropertyResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<PropertyResponse> search(PropertySearchQuery filters, Pageable pageable) {
        if (filters.minPrice() != null && filters.maxPrice() != null
                && filters.minPrice().compareTo(filters.maxPrice()) > 0) {
            throw new InvalidRequestException("minPrice must be less than or equal to maxPrice");
        }
        return repository.findAll(PropertySpecifications.matching(filters), normalize(pageable))
                .map(PropertyResponse::from);
    }

    public PropertyResponse findById(long id) {
        return locked(id, () -> cached.findById(id));
    }

    public PropertyResponse create(PropertyRequest request) {
        return transactions.create(request);
    }

    public PropertyResponse update(long id, PropertyRequest request) {
        return locked(id, () -> cached.update(id, request));
    }

    public void delete(long id) {
        locked(id, () -> {
            cached.delete(id);
            return null;
        });
    }

    private <T> T locked(long id, Supplier<T> operation) {
        if (id <= 0) throw new InvalidRequestException("id must be greater than zero");
        ReentrantLock lock = locks[Math.floorMod(Long.hashCode(id), locks.length)];
        lock.lock();
        try {
            return operation.get();
        } finally {
            lock.unlock();
        }
    }

    private Pageable normalize(Pageable pageable) {
        if (pageable.isUnpaged() || pageable.getPageSize() > 100 || pageable.getPageNumber() > 1000000) {
            throw new InvalidRequestException("Invalid pagination");
        }
        for (Sort.Order order : pageable.getSort()) {
            if (!SORT_FIELDS.contains(order.getProperty())) {
                throw new InvalidRequestException("Invalid sort field");
            }
        }
        Sort sort = pageable.getSort();
        if (sort.getOrderFor("id") == null) sort = sort.and(Sort.by("id"));
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }
}
