package br.com.fiap.imovel_Facil.property;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
public class PropertyModelAssembler implements RepresentationModelAssembler<PropertyResponse, EntityModel<PropertyResponse>> {
    @Override
    public EntityModel<PropertyResponse> toModel(PropertyResponse property) {
        return EntityModel.of(property,
                linkTo(methodOn(PropertyController.class).findById(property.id())).withSelfRel(),
                linkTo(methodOn(PropertyController.class).findAll(null, null, null)).withRel("properties"));
    }
}
