package com.sw.api.models;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Document(collection = "tareas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Tarea {

    @Id
    private String id;

    private String workflowId;

    private String estado;

    private Integer pasoActual;

    private String asignadoA;

    private String prioridad; // ALTA | MEDIA | BAJA (asignado por IA)

    private Map<String, Object> datos;

    private List<Historial> historial;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Historial {
        private String usuarioId;
        private String accion;
        private String detalle;
        private LocalDateTime fecha;
    }
}
