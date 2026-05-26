package com.sw.api.notificacion.models;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "notificaciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Notificacion {

    @Id
    private String id;

    private String usuarioId;

    private String mensaje;

    private String tipo;

    private boolean leido;

    private LocalDateTime fecha;
}
