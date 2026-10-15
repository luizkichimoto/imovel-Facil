package br.com.fiap.imovel_Facil.property;

import br.com.fiap.imovel_Facil.error.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/properties")
@Tag(name = "Imóveis", description = "Catálogo de imóveis para locação")
@ApiResponse(responseCode = "400", description = "Dados inválidos",
        content = @Content(schema = @Schema(implementation = ApiError.class)))
public class PropertyController {
    private final PropertyService service;
    private final PropertyModelAssembler assembler;

    public PropertyController(PropertyService service, PropertyModelAssembler assembler) {
        this.service = service;
        this.assembler = assembler;
    }

    @GetMapping
    @Operation(summary = "Listar imóveis com paginação", parameters = {
            @Parameter(name = "sort", description = "Campo e direção, por exemplo price,asc; padrão id,asc")})
    public PagedModel<EntityModel<PropertyResponse>> findAll(
            @Valid @ModelAttribute @ParameterObject PageQuery pagination,
            @Parameter(hidden = true) @PageableDefault(size = 10, sort = "id") Pageable pageable,
            @Parameter(hidden = true) PagedResourcesAssembler<PropertyResponse> pages) {
        return pageModel(service.findAll(pageable), pages);
    }

    @GetMapping("/example")
    @Operation(summary = "Buscar imóveis por Query by Example")
    public PagedModel<EntityModel<PropertyResponse>> example(
            @Valid @ModelAttribute @ParameterObject PropertyExampleQuery filters,
            @Valid @ModelAttribute @ParameterObject PageQuery pagination,
            @Parameter(hidden = true) @PageableDefault(size = 10, sort = "id") Pageable pageable,
            @Parameter(hidden = true) PagedResourcesAssembler<PropertyResponse> pages) {
        return pageModel(service.findByExample(filters, pageable), pages);
    }

    @GetMapping("/search")
    @Operation(summary = "Buscar imóveis por faixas e trecho do título")
    public PagedModel<EntityModel<PropertyResponse>> search(
            @Valid @ModelAttribute @ParameterObject PropertySearchQuery filters,
            @Valid @ModelAttribute @ParameterObject PageQuery pagination,
            @Parameter(hidden = true) @PageableDefault(size = 10, sort = "id") Pageable pageable,
            @Parameter(hidden = true) PagedResourcesAssembler<PropertyResponse> pages) {
        return pageModel(service.search(filters, pageable), pages);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar imóvel pelo identificador")
    @ApiResponse(responseCode = "404", description = "Imóvel não encontrado",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public EntityModel<PropertyResponse> findById(@PathVariable @Positive long id) {
        return assembler.toModel(service.findById(id));
    }

    @PostMapping
    @Operation(summary = "Cadastrar imóvel")
    @ApiResponse(responseCode = "201", description = "Imóvel criado")
    public ResponseEntity<EntityModel<PropertyResponse>> create(@Valid @RequestBody PropertyRequest request) {
        EntityModel<PropertyResponse> model = assembler.toModel(service.create(request));
        return ResponseEntity.created(URI.create(model.getRequiredLink("self").getHref())).body(model);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar imóvel e seu cache")
    @ApiResponse(responseCode = "404", description = "Imóvel não encontrado",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "Conflito de atualização",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public EntityModel<PropertyResponse> update(@PathVariable @Positive long id,
            @Valid @RequestBody PropertyRequest request) {
        return assembler.toModel(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir imóvel e remover seu cache")
    @ApiResponse(responseCode = "204", description = "Imóvel excluído", content = @Content)
    @ApiResponse(responseCode = "404", description = "Imóvel não encontrado",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ResponseEntity<Void> delete(@PathVariable @Positive long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    private PagedModel<EntityModel<PropertyResponse>> pageModel(Page<PropertyResponse> page,
            PagedResourcesAssembler<PropertyResponse> pages) {
        pages.setForceFirstAndLastRels(true);
        return pages.toModel(page, assembler);
    }
}
