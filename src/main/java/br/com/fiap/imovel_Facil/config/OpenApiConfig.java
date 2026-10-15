package br.com.fiap.imovel_Facil.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {
    @Bean
    public OpenAPI propertyOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Imóvel Fácil API")
                .version("1.0.0")
                .description("API de catálogo de imóveis com paginação, buscas, HATEOAS e cache."));
    }
}
