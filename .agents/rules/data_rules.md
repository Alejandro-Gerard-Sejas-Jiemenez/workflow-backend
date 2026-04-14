---
trigger: always_on
---

# Description
Reglas para el manejo de persistencia en MongoDB y estandarización de DTOs.

# Content
Este módulo define las normas para la gestión de datos y la comunicación entre capas.

## 📦 Estándares de MongoDB
- **Uso de Embebidos:** Los `pasos` en un Workflow y el `historial` en una Tarea deben ser colecciones embebidas (Listas dentro del documento) para optimizar el rendimiento.
- **Uso de Referencias:** Los enlaces entre Tareas y Usuarios/Workflows deben ser mediante `ObjectId` para mantener la integridad relacional mínima necesaria.
- **Campo Dinámico:** El campo `datos` en la `Tarea` siempre debe implementarse como un `Map<String, Object>` para soportar formularios dinámicos.

## 📝 Validaciones y DTOs
- **Validaciones:** Usar anotaciones de Jakarta Bean Validation (ej. `@NotBlank`, `@Min`, `@NotNull`).
- **DTOs:** Utilizar objetos de transferencia para:
  - `RequestDTO`: Entrada del usuario (limpiar datos sensibles).
  - `ResponseDTO`: Salida de la API (no exponer contraseñas ni IDs internos si no es necesario).
- **Consistencia:** Asegurar que los nombres de los campos en los DTOs coincidan con los nombres de los campos definidos en la colección `Formularios`.
