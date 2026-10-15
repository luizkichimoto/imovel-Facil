package br.com.fiap.imovel_Facil.property;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class CachedPropertyService {
    private final PropertyTransactions transactions;

    public CachedPropertyService(PropertyTransactions transactions) {
        this.transactions = transactions;
    }

    @Cacheable(cacheNames = "properties", key = "#id")
    public PropertyResponse findById(long id) {
        return transactions.findById(id);
    }

    @CachePut(cacheNames = "properties", key = "#id")
    public PropertyResponse update(long id, PropertyRequest request) {
        return transactions.update(id, request);
    }

    @CacheEvict(cacheNames = "properties", key = "#id")
    public void delete(long id) {
        transactions.delete(id);
    }
}
