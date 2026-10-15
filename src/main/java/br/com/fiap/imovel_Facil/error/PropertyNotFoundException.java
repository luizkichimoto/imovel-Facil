package br.com.fiap.imovel_Facil.error;

public class PropertyNotFoundException extends RuntimeException {
    public PropertyNotFoundException() {
        super("Property not found");
    }
}
