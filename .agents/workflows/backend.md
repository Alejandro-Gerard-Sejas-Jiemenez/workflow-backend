---
description: Guía central de arquitectura y stack tecnológico para el backend (Java 21, Spring Boot, MongoDB).
---

# Description
Guía central de arquitectura y stack tecnológico para el backend (Java 21, Spring Boot, MongoDB).

# Content
Este es el archivo principal de referencia para el desarrollo del backend. Define el stack de base y cómo se organizan los módulos de reglas.

## 🛠️ Stack Tecnológico
- **Java:** Versión 21.
- **Framework:** Spring Boot.
- **Base de Datos:** MongoDB.
- **Seguridad:** Spring Security con JWT.

## 📁 Guía de Arquitectura
Respetar estrictamente la estructura de paquetes:
- `com.sw.api.controllers`: Endpoints (Reglas en `security_rules.md`).
- `com.sw.api.services`: Lógica de negocio.
- `com.sw.api.repositories`: Repositorios Mongo (Reglas en `data_rules.md`).
- `com.sw.api.models`: Dominio en español (Reglas en `data_rules.md`).
- `com.sw.api.security`: Configuración técnica de seguridad.

## 📚 Módulos de Reglas (Customized)
Para un desarrollo coherente, consulta siempre las reglas específicas:
- **`security_rules.md`**: Manejo de actores y permisos.
- **`data_rules.md`**: Persistencia en MongoDB y DTOs.
- **`engine_rules.md`**: Lógica del motor de workflows y SpEL.

## 🚀 Flujo de Desarrollo Recomendado
1. Consultar el modelo en `models`.
2. Definir la lógica en `services` asegurando la auditoría (`engine_rules.md`).
3. Exponer el servicio en `controllers` con la seguridad adecuada (`security_rules.md`).
4. Probar en Swagger y actualizar la documentación de endpoints.
