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
    private String solicitanteId;

    private String estado;

    private Integer pasoActual;

    private String asignadoA;

    private String prioridad; // ALTA | MEDIA | BAJA (asignado por IA)
    
    private List<String> documentosUrl;

    private Map<String, Object> datos;

    private List<Historial> historial;

    private List<Comentario> comentarios;

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

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Comentario {
        private String id;           // UUID generado en código
        private String usuarioId;    // Quién comenta
        private String contenido;    // Texto del comentario
        private List<String> menciones; // @usuarioIds mencionados
        private String archivoUrl;   // URL en Cloudinary (opcional)
        private LocalDateTime fecha;
    }
}
