package br.com.fiap.imovel_Facil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.fiap.imovel_Facil.config.DataLoader;
import br.com.fiap.imovel_Facil.property.PropertyRepository;
import br.com.fiap.imovel_Facil.property.PropertyRequest;
import br.com.fiap.imovel_Facil.property.PropertyResponse;
import br.com.fiap.imovel_Facil.property.PropertyService;
import br.com.fiap.imovel_Facil.property.PropertyTransactions;
import br.com.fiap.imovel_Facil.property.PropertyType;
import java.math.BigDecimal;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = "app.seed.enabled=false")
@AutoConfigureMockMvc
class PropertyApiTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired PropertyService service;
    @Autowired PropertyRepository repository;
    @Autowired CacheManager cacheManager;
    @MockitoSpyBean PropertyTransactions transactions;
    private PropertyResponse apartment;

    @BeforeEach
    void setUp() {
        Objects.requireNonNull(cacheManager.getCache("properties")).clear();
        repository.deleteAllInBatch();
        apartment = service.create(request("Apartamento com varanda", PropertyType.APARTMENT,
                "São Paulo", "Pinheiros", "3200.00", 2, "68.50"));
        service.create(request("Casa com jardim", PropertyType.HOUSE,
                "São Paulo", "Vila Madalena", "5000.00", 3, "120.00"));
        service.create(request("Studio compacto", PropertyType.STUDIO,
                "Campinas", "Cambuí", "1800.00", 0, "30.00"));
        service.create(request("Apartamento central", PropertyType.APARTMENT,
                "São Paulo", "Centro", "2500.00", 1, "45.00"));
        clearInvocations(transactions);
    }

    @Test
    void listsDefaultPageWithRequiredMetadataAndLinks() throws Exception {
        mvc.perform(get("/properties").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.size").value(10))
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.totalElements").value(4))
                .andExpect(jsonPath("$.page.totalPages").value(1))
                .andExpect(jsonPath("$._embedded.properties.length()").value(4))
                .andExpect(jsonPath("$._embedded.properties[0].id").value(apartment.id().intValue()))
                .andExpect(jsonPath("$._embedded.properties[0]._links.self.href", endsWith("/properties/" + apartment.id())))
                .andExpect(jsonPath("$._embedded.properties[0]._links.properties.href", endsWith("/properties")))
                .andExpect(jsonPath("$._links.first.href").exists())
                .andExpect(jsonPath("$._links.last.href").exists())
                .andExpect(jsonPath("$._links.prev").doesNotExist())
                .andExpect(jsonPath("$._links.next").doesNotExist());
    }

    @Test
    void paginatesAndSortsByPrice() throws Exception {
        mvc.perform(get("/properties").param("page", "1").param("size", "2").param("sort", "price,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.number").value(1))
                .andExpect(jsonPath("$.page.totalPages").value(2))
                .andExpect(jsonPath("$._embedded.properties[0].price").value(2500.0))
                .andExpect(jsonPath("$._embedded.properties[1].price").value(1800.0));
    }

    @Test
    void exampleMatchesExactTextIgnoringCaseAndIgnoresMissingFields() throws Exception {
        mvc.perform(get("/properties/example").param("type", "APARTMENT")
                        .param("city", "sÃO pAULO").param("neighborhood", "PINHEIROS").param("bedrooms", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$._embedded.properties[0].id").value(apartment.id().intValue()));
        mvc.perform(get("/properties/example").param("city", "São"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(0));
        mvc.perform(get("/properties/example"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(4));
    }

    @Test
    void exampleHonorsZeroBedrooms() throws Exception {
        mvc.perform(get("/properties/example").param("bedrooms", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$._embedded.properties[0].type").value("STUDIO"));
    }

    @Test
    void searchCombinesEveryFilterAndIncludesPriceAndAreaBoundaries() throws Exception {
        mvc.perform(get("/properties/search").param("title", "VARANDA").param("city", "são paulo")
                        .param("minPrice", "3200.00").param("maxPrice", "3200.00")
                        .param("minBedrooms", "2").param("minArea", "68.50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$._embedded.properties[0].id").value(apartment.id().intValue()));
        mvc.perform(get("/properties/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(4));
    }

    @Test
    void emptyPageOmitsEmbedded() throws Exception {
        mvc.perform(get("/properties/search").param("city", "Cidade inexistente"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(0))
                .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @ParameterizedTest
    @CsvSource({"/properties/example", "/properties/search"})
    void navigationRetainsFilters(String path) throws Exception {
        mvc.perform(get(path).queryParam("city", "São Paulo").queryParam("page", "1").queryParam("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(3))
                .andExpect(jsonPath("$._links.self.href", containsString("city=")))
                .andExpect(jsonPath("$._links.first.href", containsString("city=")))
                .andExpect(jsonPath("$._links.prev.href", containsString("city=")))
                .andExpect(jsonPath("$._links.next.href", containsString("city=")))
                .andExpect(jsonPath("$._links.last.href", containsString("city=")));
    }

    @Test
    void searchTreatsSqlAndLikeMetacharactersAsLiteralText() throws Exception {
        service.create(request("Oferta 100%_especial", PropertyType.APARTMENT,
                "São Paulo", "Centro", "2100.00", 1, "40.00"));
        mvc.perform(get("/properties/search").param("title", "%_"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1));
        mvc.perform(get("/properties/search").param("title", "' OR 1=1 --"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @ParameterizedTest
    @CsvSource({
            "/properties,page,-1", "/properties,page,abc", "/properties,page,1000001",
            "/properties,size,0", "/properties,size,101", "/properties,size,abc",
            "/properties,sort,version", "/properties,sort,unknown",
            "/properties/example,type,INVALID", "/properties/example,bedrooms,-1",
            "/properties/search,minPrice,-1", "/properties/search,minPrice,abc",
            "/properties/search,minBedrooms,-1", "/properties/search,minArea,-1"
    })
    void rejectsInvalidQueryParameters(String path, String parameter, String value) throws Exception {
        mvc.perform(get(path).param(parameter, value))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isString());
    }

    @Test
    void rejectsInvertedPriceRange() throws Exception {
        mvc.perform(get("/properties/search").param("minPrice", "5000").param("maxPrice", "1000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("minPrice must be less than or equal to maxPrice"));
    }

    @Test
    void createsPropertyWithLocationAndLinks() throws Exception {
        mvc.perform(post("/properties").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(request("  Novo imóvel  ", PropertyType.HOUSE,
                                "São Paulo", "Pinheiros", "4200.00", 3, "100.00"))))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.title").value("Novo imóvel"))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.properties.href").exists())
                .andExpect(jsonPath("$.version").doesNotExist());
        assertThat(repository.count()).isEqualTo(5);
    }

    @Test
    void rejectsInvalidBodiesWithoutWriting() throws Exception {
        mvc.perform(post("/properties").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\" \"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").isString());
        mvc.perform(post("/properties").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").isString());
        assertThat(repository.count()).isEqualTo(4);
    }

    @Test
    void rejectsClientSuppliedIdAndFractionalBedrooms() throws Exception {
        String valid = json.writeValueAsString(request("Novo", PropertyType.HOUSE,
                "São Paulo", "Centro", "3000.00", 2, "80.00"));
        mvc.perform(post("/properties").contentType(MediaType.APPLICATION_JSON)
                        .content(valid.replaceFirst("\\{", "{\"id\":1,")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/properties").contentType(MediaType.APPLICATION_JSON)
                        .content(valid.replace("\"bedrooms\":2", "\"bedrooms\":2.5")))
                .andExpect(status().isBadRequest());
        assertThat(repository.count()).isEqualTo(4);
    }

    @Test
    void cachesDetailUpdatesEntryAndEvictsOnDelete() throws Exception {
        String path = "/properties/" + apartment.id();
        mvc.perform(get(path)).andExpect(status().isOk());
        mvc.perform(get(path)).andExpect(status().isOk());
        verify(transactions, times(1)).findById(apartment.id());
        mvc.perform(put(path).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(request("Título atualizado", PropertyType.APARTMENT,
                                "São Paulo", "Pinheiros", "3500.00", 2, "68.50"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Título atualizado"));
        mvc.perform(get(path)).andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Título atualizado"));
        verify(transactions, times(1)).findById(apartment.id());
        mvc.perform(delete(path)).andExpect(status().isNoContent()).andExpect(content().string(""));
        assertThat(Objects.requireNonNull(cacheManager.getCache("properties")).get(apartment.id())).isNull();
        mvc.perform(get(path)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Property not found"));
        verify(transactions, times(2)).findById(apartment.id());
    }

    @Test
    void failedValidationDoesNotModifyDatabaseOrCachedDetail() throws Exception {
        String path = "/properties/" + apartment.id();
        mvc.perform(get(path)).andExpect(status().isOk());
        mvc.perform(put(path).contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get(path)).andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(apartment.title()));
        assertThat(repository.findById(apartment.id()).orElseThrow().getTitle()).isEqualTo(apartment.title());
    }

    @Test
    void rolledBackUpdateLeavesDatabaseAndCacheUnchanged() {
        service.findById(apartment.id());
        PropertyRequest invalid = request("Não deve persistir", PropertyType.APARTMENT,
                "São Paulo", "Pinheiros", "9999999999999999.00", 2, "68.50");
        assertThatThrownBy(() -> service.update(apartment.id(), invalid))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(service.findById(apartment.id()).title()).isEqualTo(apartment.title());
        assertThat(repository.findById(apartment.id()).orElseThrow().getTitle()).isEqualTo(apartment.title());
    }

    @Test
    void missingPropertyReturns404ForEveryOperation() throws Exception {
        String path = "/properties/9223372036854775807";
        mvc.perform(get(path)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Property not found"));
        mvc.perform(put(path).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(request("Novo", PropertyType.HOUSE,
                                "São Paulo", "Centro", "3000.00", 2, "80.00"))))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("Property not found"));
        mvc.perform(delete(path)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Property not found"));
    }

    @Test
    void rejectsInvalidIdentifiers() throws Exception {
        mvc.perform(get("/properties/0")).andExpect(status().isBadRequest());
        mvc.perform(get("/properties/-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/properties/abc")).andExpect(status().isBadRequest());
    }

    @Test
    void loadsSixtyPropertiesWithoutDuplicatingThem() {
        repository.deleteAllInBatch();
        DataLoader loader = new DataLoader(repository);
        loader.run();
        assertThat(repository.count()).isEqualTo(60);
        assertThat(repository.findAll()).anyMatch(property -> property.getTitle().equals("Apto com varanda"));
        loader.run();
        assertThat(repository.count()).isEqualTo(60);
    }

    @Test
    void exposesOnlyRequestedReadOnlyActuators() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"UP\"}"));
        mvc.perform(get("/actuator/info")).andExpect(status().isOk())
                .andExpect(jsonPath("$.app.name").value("Imóvel Fácil API"))
                .andExpect(jsonPath("$.app.version").value("1.0.0"));
        mvc.perform(get("/actuator/caches")).andExpect(status().isOk())
                .andExpect(jsonPath("$.cacheManagers.cacheManager.caches.properties").exists());
        mvc.perform(get("/actuator/env")).andExpect(status().isNotFound());
        mvc.perform(get("/actuator/beans")).andExpect(status().isNotFound());
        mvc.perform(get("/actuator/metrics")).andExpect(status().isNotFound());
        mvc.perform(delete("/actuator/caches")).andExpect(status().isMethodNotAllowed());
        mvc.perform(get("/h2-console")).andExpect(status().isNotFound());
    }

    @Test
    void servesSwaggerAndOpenApi() throws Exception {
        mvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Imóvel Fácil API"))
                .andExpect(jsonPath("$.paths['/properties/search'].get").exists())
                .andExpect(jsonPath("$.paths['/properties/example'].get").exists());
    }

    @Test
    void allowsLocalCorsAndRejectsUntrustedOrigin() throws Exception {
        mvc.perform(options("/properties").header("Origin", "http://localhost:8081")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:8081"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
        mvc.perform(options("/properties").header("Origin", "https://untrusted.example")
                        .header("Access-Control-Request-Method", "DELETE"))
                .andExpect(status().isForbidden());
    }

    private PropertyRequest request(String title, PropertyType type, String city, String neighborhood,
            String price, int bedrooms, String area) {
        return new PropertyRequest(title, "Descrição do imóvel", type, city, neighborhood,
                new BigDecimal(price), bedrooms, new BigDecimal(area));
    }
}
