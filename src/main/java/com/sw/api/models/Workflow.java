package com.sw.api.models;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Document(collection = "workflows")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Workflow {

    @Id
    private String id;

    private String nombre;

    private String descripcion;

    private List<Paso> pasos;

    private List<Regla> reglas;
    
    private String diagramData;

    private String ownerUserId;

    private List<Collaborator> collaborators;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Paso {
        private String nombre;
        private Integer orden;
        private String departamento;
        private String formularioId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Regla {
        private String condicion;
        private String accion;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Collaborator {
        private String userId;
        private String role;
    }
}
