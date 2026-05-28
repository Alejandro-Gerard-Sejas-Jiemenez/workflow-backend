package com.sw.api.formulario.models;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;
import java.util.Map;

@Document(collection = "formularios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Formulario {

    @Id
    private String id;

    private String nombre;

    private String descripcion;

    private List<Campo> campos;

    private List<Object> reglasVisibilidad;

    private boolean allowAttachments;

    private String allowedTypes;

    private String requiredDocs;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Campo {
        private String nombre;
        private String tipo;
        private boolean requerido;
        private Object opciones;
    }
}
