-- 1. SOLUCIÓN AL ERROR: Quitar la restricción única para que varios sean 'PERSONAL' o 'EMPRESA'
ALTER TABLE perfil DROP CONSTRAINT perfil_id_tipo_perfil_key;

-- 2. Crear la nueva tabla de roles de seguridad
CREATE TABLE rol (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(50) NOT NULL UNIQUE
);

-- 3. Insertar los roles base del sistema
INSERT INTO rol (nombre) VALUES ('ROLE_USER');
INSERT INTO rol (nombre) VALUES ('ROLE_ADMIN');

-- 4. Agregar la columna a la tabla usuario (sin NOT NULL al inicio por si ya tienes datos)
ALTER TABLE usuario ADD COLUMN id_rol UUID;

-- 5. Crear la relación (Llave foránea) de usuario hacia rol
ALTER TABLE usuario ADD CONSTRAINT fk_usuario_rol FOREIGN KEY (id_rol) REFERENCES rol (id);