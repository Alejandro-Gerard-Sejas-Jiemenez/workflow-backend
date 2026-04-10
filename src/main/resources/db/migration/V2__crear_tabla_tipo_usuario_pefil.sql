CREATE TABLE tipos_perfil (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(50) NOT NULL UNIQUE,

    created_by UUID,
    created_date TIMESTAMP NOT NULL DEFAULT NOW(),
    last_modified_by UUID,
    last_modified_date TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE 
);

CREATE TABLE perfil (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    foto_url VARCHAR(255),
    id_usuario UUID NOT NULL UNIQUE,
    id_tipo_perfil UUID NOT NULL UNIQUE,
    
    created_by UUID,
    created_date TIMESTAMP NOT NULL DEFAULT NOW(),
    last_modified_by UUID,
    last_modified_date TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
 
    CONSTRAINT fk_perfil_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id),
    CONSTRAINT fk_perfil_tipo FOREIGN KEY (id_tipo_perfil) REFERENCES tipos_perfil (id)
);

INSERT INTO tipos_perfil (nombre) VALUES ('PERSONAL');
INSERT INTO tipos_perfil (nombre) VALUES ('EMPRESA');