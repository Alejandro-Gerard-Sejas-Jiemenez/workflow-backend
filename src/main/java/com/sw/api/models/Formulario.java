package com.sw.api.models;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Document(collection = "formularios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Formulario {

    @Id
    private String id;

    private String nombre;

    private List<Campo> campos;

    private List<Object> reglasVisibilidad;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Campo {
        private String nombre;
        private String tipo;
        private boolean requerido;
    }
}
