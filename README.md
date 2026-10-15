# Imóvel Fácil API

API REST de catálogo de imóveis para locação, com Java 25, Spring Boot 4.1.1 e Gradle 9.8.1.

## Executar

É necessário ter um JDK 25 instalado, com `JAVA_HOME` apontando para ele. Na primeira execução, o Gradle Wrapper precisa de acesso à internet para baixar o Gradle e as dependências. Não é necessário instalar o Gradle separadamente nem configurar um banco externo.

No PowerShell, dentro da pasta do projeto:

```powershell
java -version
.\gradlew.bat bootRun
```

No Linux ou macOS:

```bash
java -version
./gradlew bootRun
```

A API inicia em `http://localhost:8080`. O banco H2 fica em memória e recebe automaticamente 60 imóveis demonstrativos. Os dados são recriados ao reiniciar a aplicação.

| Recurso | Endereço |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| Catálogo | http://localhost:8080/properties |
| Saúde | http://localhost:8080/actuator/health |
| Informações | http://localhost:8080/actuator/info |
| Caches | http://localhost:8080/actuator/caches |

## Funcionalidades

- CRUD de imóveis com DTOs de entrada e saída, validação e tratamento centralizado de erros.
- Paginação na listagem e nas buscas, com padrão de 10 registros por página e ordenação por `id`.
- Query by Example para igualdade de tipo, cidade, bairro e quantidade de quartos.
- Specifications para combinar título parcial, cidade, faixa de preço, quantidade mínima de quartos e área mínima.
- HATEOAS com links individuais e navegação das páginas preservando os filtros.
- Cache Caffeine no detalhe, atualizado no PUT e removido no DELETE.
- Swagger/OpenAPI e somente `health`, `info` e `caches` expostos no Actuator, com acesso de leitura.
- Testes de integração com MockMvc, H2 e o cache real.

## Contrato HTTP

| Método | Rota | Resultado |
|---|---|---|
| GET | `/properties` | 200 com página de imóveis |
| GET | `/properties/example` | 200 com página filtrada por exemplo |
| GET | `/properties/search` | 200 com página filtrada por Specification |
| GET | `/properties/{id}` | 200 com imóvel ou 404 |
| POST | `/properties` | 201 com imóvel, links e cabeçalho `Location`; 400 para entrada inválida |
| PUT | `/properties/{id}` | 200 com imóvel atualizado; 400, 404 ou 409 |
| DELETE | `/properties/{id}` | 204 sem corpo; 400, 404 ou 409 |

POST e PUT recebem um objeto sem `id`, `version` ou links:

```json
{
  "title": "Apto com varanda",
  "description": "Perto do metrô",
  "type": "APARTMENT",
  "city": "São Paulo",
  "neighborhood": "Pinheiros",
  "price": 3200.00,
  "bedrooms": 2,
  "area": 68.5
}
```

`type` aceita `APARTMENT`, `HOUSE`, `STUDIO` ou `COMMERCIAL`. `price` representa o aluguel mensal em reais; `area`, a área em metros quadrados. Ambos usam `BigDecimal` e aceitam no máximo duas casas decimais. `bedrooms` aceita valores inteiros de 0 a 100. `description` é opcional e tem limite de 2.000 caracteres. Os demais campos são obrigatórios; título, cidade e bairro não podem estar em branco.

Campos JSON desconhecidos, IDs enviados no corpo, números fracionários em `bedrooms`, preços e áreas não positivos geram 400. Espaços nas extremidades dos textos são removidos, sem alterar acentos ou o conteúdo interno.

Um imóvel contém `_links.self` e `_links.properties`. Páginas usam `_embedded.properties` e `page` com `size`, `totalElements`, `totalPages` e `number`. Uma página vazia não contém `_embedded`. Links `first`, `self` e `last` são gerados; `prev` e `next` aparecem quando existe página anterior ou seguinte.

Imóvel inexistente:

```json
{"error":"Property not found"}
```

Erros de validação também usam o campo `error`, com uma descrição apropriada. Exceções internas não expõem stack traces nem mensagens do banco nas respostas.

## Paginação e filtros

`page` começa em 0; `size` deve ficar entre 1 e 100. O limite de `page` é 1.000.000. A ordenação padrão é `id,asc`. A ordenação aceita `id`, `title`, `type`, `city`, `neighborhood`, `price`, `bedrooms` e `area`. Um desempate por `id` mantém a ordem determinística para valores iguais.

Exemplos de caminhos:

```text
/properties?page=0&size=10
/properties?page=0&size=5&sort=price,desc
/properties/example?type=APARTMENT&city=S%C3%A3o%20Paulo&bedrooms=2
/properties/search?title=varanda&minPrice=2000&maxPrice=4000&minBedrooms=2&minArea=60
```

| Busca | Filtro | Regra |
|---|---|---|
| Example | `type` | Igualdade do enum |
| Example | `city`, `neighborhood` | Igualdade sem diferenciar maiúsculas de minúsculas |
| Example | `bedrooms` | Igualdade, incluindo zero |
| Search | `title` | Contém o texto literal, sem diferenciar maiúsculas de minúsculas |
| Search | `city` | Igualdade sem diferenciar maiúsculas de minúsculas |
| Search | `minPrice`, `maxPrice` | Limites inclusivos |
| Search | `minBedrooms`, `minArea` | Limites mínimos inclusivos |

Todos os filtros são opcionais e combinados com AND. Textos vazios são ignorados. `minPrice` não pode exceder `maxPrice`. Acentos continuam significativos. `%`, `_` e barra invertida no título são tratados literalmente; não funcionam como curingas fornecidos pelo cliente.

## Cache e transações

O cache `properties` armazena DTOs imutáveis, sem entidades JPA nem links dependentes da requisição. Seu limite é de 1.000 entradas, com expiração de 10 minutos após a escrita.

`CachedPropertyService` aplica `@Cacheable`, `@CachePut` e `@CacheEvict`. `PropertyTransactions` executa o acesso ao banco em outra instância gerenciada pelo Spring; assim, a transação termina antes da alteração do cache. `PropertyService` coordena leituras e escritas do mesmo ID com 64 locks compartilhados por grupos de IDs, evitando que uma leitura concorrente volte a gravar um valor antigo no cache após PUT ou DELETE.

Essa coordenação vale para uma única instância da aplicação, usando os serviços fornecidos. Há uma pequena contenção entre IDs que compartilham o mesmo lock. Uma implantação com várias instâncias e banco compartilhado precisaria de uma estratégia de invalidação entre instâncias. O cache deste projeto é local.

Para observar o cache em uma execução recém-iniciada:

1. Faça `GET /properties/7`: o console registra `Consultando imóvel 7 no banco de dados`.
2. Repita o GET: o detalhe vem do cache e a mensagem não se repete.
3. Faça PUT no mesmo ID com o corpo completo e consulte novamente: o GET retorna a atualização.
4. Faça DELETE e consulte novamente: o GET responde 404.

## Testes e empacotamento

PowerShell:

```powershell
.\gradlew.bat clean test
.\gradlew.bat bootJar
java -jar .\build\libs\imovel-Facil-1.0.0.jar
```

Linux ou macOS:

```bash
./gradlew clean test
./gradlew bootJar
java -jar build/libs/imovel-Facil-1.0.0.jar
```

Após a execução dos testes, o relatório fica em `build/reports/tests/test/index.html`.

A suíte cobre paginação, ordenação, buscas combinadas, filtros preservados nos links, página vazia, metacaracteres de SQL e LIKE, corpos inválidos, tentativa de envio de ID, criação, atualização, exclusão, coerência do cache, carga inicial, CORS, OpenAPI e exposição do Actuator.

**Estado da validação desta entrega:** os arquivos Java passaram por análise sintática com o parser do JDK disponível; o JAR do Gradle Wrapper teve o SHA-256 conferido contra a referência oficial. A compilação e a suíte de integração não foram executadas: o ambiente de preparação tinha somente JDK 17, e a tentativa de `./gradlew test --no-daemon` falhou no download do Gradle com `Network is unreachable`. A análise sintática não substitui compilação, resolução de dependências nem testes com JDK 25.

## Execução local e acesso por outro dispositivo

Por padrão, o servidor escuta apenas em `127.0.0.1`. O CORS permite origens HTTP de `localhost` e `127.0.0.1`, sem credenciais. Aplicativos nativos normalmente não dependem de CORS.

Para testar em um dispositivo da mesma rede, é possível definir o endereço de escuta antes de executar:

```powershell
$env:SERVER_ADDRESS = "0.0.0.0"
.\gradlew.bat bootRun
```

No dispositivo, use `http://IP_DA_MAQUINA:8080`. Para uma interface web em outra origem, configure `CORS_ALLOWED_ORIGIN_PATTERNS` com as origens necessárias, separadas por vírgula.

O contrato não inclui autenticação. A configuração entregue destina-se a execução local; habilitar acesso pela rede permite que os clientes alcançáveis usem também POST, PUT e DELETE. CORS não substitui autorização. Para publicação, seriam necessários autenticação, autorização, HTTPS e banco persistente. O console H2 permanece desabilitado.

## Organização

| Pacote | Responsabilidade |
|---|---|
| `property` | Entidade, DTOs, filtros, repositório, serviços, controller e montagem de HATEOAS |
| `config` | Carga inicial, CORS e OpenAPI |
| `error` | Exceções e respostas padronizadas |

O projeto mantém o Gradle e o pacote Java da base enviada. O checksum da distribuição está fixado em `gradle/wrapper/gradle-wrapper.properties`. Os scripts gerados do Gradle preservam seus avisos de licença; os arquivos Java não contêm comentários.

## Integrantes

| Gabriel Rocha de Souza| RM567023  |
|---------------------------------|-----------|
| Luis Fillipe Rodrigues Seripieri| RM567918  |
| Luiz Felipe Kichimoto Valdevino | RM567726   |


## Referências técnicas

- https://docs.spring.io/spring-data/jpa/reference/query-by-example.html
- https://docs.spring.io/spring-data/jpa/reference/jpa/specifications.html
- https://docs.spring.io/spring-hateoas/reference/
- https://docs.spring.io/spring-boot/reference/io/caching.html
- https://docs.spring.io/spring-boot/reference/actuator/endpoints.html
- https://springdoc.org/
- https://gradle.org/release-checksums/
