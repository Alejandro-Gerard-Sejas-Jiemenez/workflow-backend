package com.sw.api.usuario.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.sw.api.bitacora.models.Auditable;

@Document(collection = "departamentos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Departamento extends Auditable {

    @Id
    private String id;

    private String nombre;

    private String descripcion;

    private String estado = "ACTIVO"; // ACTIVO, INACTIVO
}
