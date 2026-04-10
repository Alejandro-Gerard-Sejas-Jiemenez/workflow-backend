CREATE TABLE ubicacion (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_inmueble UUID NOT NULL UNIQUE,
    ciudad VARCHAR(100) NOT NULL,
    zona_barrios VARCHAR(100),
    direccion_exacta VARCHAR(255),
    latitud VARCHAR(50),
    longitud VARCHAR(50),
    
    -- Campos de auditoría
    created_by UUID,
    created_date TIMESTAMP NOT NULL DEFAULT NOW(),
    last_modified_by UUID,
    last_modified_date TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    
    CONSTRAINT fk_ubicacion_inmueble FOREIGN KEY (id_inmueble) REFERENCES inmueble (id)
);

CREATE TABLE imagen_publicacion (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_publicacion UUID NOT NULL,
    url_image VARCHAR(500) NOT NULL,
    es_portada BOOLEAN DEFAULT FALSE,
    
    -- Campos de auditoría
    created_by UUID,
    created_date TIMESTAMP NOT NULL DEFAULT NOW(),
    last_modified_by UUID,
    last_modified_date TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    
    CONSTRAINT fk_imagen_publicacion FOREIGN KEY (id_publicacion) REFERENCES publicacion (id)
);
