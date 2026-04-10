CREATE TABLE inmueble (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tipo_inmueble VARCHAR(50) NOT NULL,
    area_terreno DECIMAL(10,2),
    area_construida DECIMAL(10,2),
    habitaciones INT,
    banos INT,
    garajes INT,
    antiguedad_anios INT,
    
    -- Campos de auditoría
    created_by UUID,
    created_date TIMESTAMP NOT NULL DEFAULT NOW(),
    last_modified_by UUID,
    last_modified_date TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

CREATE TABLE publicacion (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_usuario UUID NOT NULL,
    id_inmueble UUID NOT NULL,
    titulo VARCHAR(255) NOT NULL,
    descripcion_general TEXT,
    tipo_transaccion VARCHAR(50) NOT NULL,
    precio DECIMAL(15,2) NOT NULL,
    moneda VARCHAR(10) NOT NULL DEFAULT 'USD',
    estado_publicacion VARCHAR(50) NOT NULL DEFAULT 'ACTIVO',
    fecha_publicacion TIMESTAMP DEFAULT NOW(),
    
    -- Campos de auditoría
    created_by UUID,
    created_date TIMESTAMP NOT NULL DEFAULT NOW(),
    last_modified_by UUID,
    last_modified_date TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,

    CONSTRAINT fk_publicacion_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id),
    CONSTRAINT fk_publicacion_inmueble FOREIGN KEY (id_inmueble) REFERENCES inmueble (id)
);
