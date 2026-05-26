package com.sw.api.bitacora.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "bitacora")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Bitacora {

    @Id
    private String id;

    private String usuarioId;
    
    private String usuarioNombre;
    
    private String accion;
    
    private String entidad;
    
    private LocalDateTime fecha = LocalDateTime.now();
    
    private String detalles;
}
