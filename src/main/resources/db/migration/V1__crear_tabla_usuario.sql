CREATE TABLE usuario (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    correo VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    estado_conexion BOOLEAN DEFAULT FALSE,
    ultima_conexion TIMESTAMP,
    
    -- Campos de auditoría
    created_by UUID,
    created_date TIMESTAMP NOT NULL DEFAULT NOW(),
    last_modified_by UUID,
    last_modified_date TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE 
);