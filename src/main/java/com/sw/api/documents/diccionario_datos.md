# Diccionario de Datos - Workflow Engine Backend

Este documento detalla la estructura de datos de los principales módulos del sistema implementados en MongoDB.

---

## 👥 Módulo: Usuarios (`usuarios`)
Gestiona la información de los actores del sistema y su estado.

| Atributo | Tipo de Dato | Descripción |
| :--- | :--- | :--- |
| `id` | `ObjectId` (String) | Identificador único del usuario. |
| `nombre` | `String` | Nombre(s) del usuario. |
| `apellido` | `String` | Apellidos del usuario. |
| `email` | `String` | Correo electrónico (usado para login). |
| `password` | `String` | Contraseña cifrada (BCrypt). |
| `departamento` | `String` | Departamento al que pertenece (ej. RRHH). |
| `telefono` | `String` | Número de contacto del usuario. |
| `activo` | `Boolean` | **Borrado Lógico**: Indica si el usuario está activo o "eliminado". |
| `estadoConexion` | `Boolean` | Indica si el usuario está actualmente activo. |
| `ultimaConexion` | `ISODate` | Fecha y hora del último acceso. |
| `rol` | `DBRef` (Rol) | Referencia al rol asignado (Admin, Empleado, etc). |

---

## 🔄 Módulo: Workflows (`workflows`)
Define la estructura de los procesos de negocio.

| Atributo | Tipo de Dato | Descripción |
| :--- | :--- | :--- |
| `id` | `ObjectId` (String) | Identificador único del workflow. |
| `nombre` | `String` | Título descriptivo del proceso. |
| `descripcion` | `String` | Detalle extendido del propósito del workflow. |
| `pasos` | `Array<Paso>` | **Embebido**: Lista ordenada de etapas del proceso. |
| `reglas` | `Array<Regla>` | **Embebido**: Lógica SpEL para transiciones dinámicas. |

### 🧩 Sub-objeto: Paso
- `nombre` (String)
- `orden` (Integer)
- `departamento` (String): Quién debe procesarlo.
- `formularioId` (String): ID del formulario asociado.

---

## 📄 Módulo: Formularios (`formularios`)
Define los campos dinámicos para la recolección de datos.

| Atributo | Tipo de Dato | Descripción |
| :--- | :--- | :--- |
| `id` | `ObjectId` (String) | Identificador único del formulario. |
| `nombre` | `String` | Nombre del formulario. |
| `campos` | `Array<Campo>` | **Embebido**: Definición de los campos (nombre, tipo, requerido). |

---

## 📋 Módulo: Tareas (`tareas`)
Instancia de un workflow en ejecución.

| Atributo | Tipo de Dato | Descripción |
| :--- | :--- | :--- |
| `id` | `ObjectId` (String) | Identificador único de la instancia. |
| `workflowId` | `ObjectId` (String) | Referencia al workflow de origen. |
| `estado` | `String` | Estado actual (PENDIENTE, EN_PROCESO, FINALIZADO). |
| `pasoActual` | `Integer` | Índice del paso en el que se encuentra. |
| `asignadoA` | `ObjectId` (String) | Usuario actualmente responsable. |
| `datosBase` | `Map<String, Object>` | **Dinámico**: Datos recolectados en los formularios. |
| `historial` | `Array<Historial>` | **Embebido**: Auditoría de cada cambio realizado. |

---

## 🔔 Módulo: Notificaciones (`notificaciones`)
Alertas para los usuarios sobre cambios en sus tareas.

| Atributo | Tipo de Dato | Descripción |
| :--- | :--- | :--- |
| `id` | `ObjectId` (String) | Identificador único de la notificación. |
| `usuarioId` | `ObjectId` (String) | Destinatario de la alerta. |
| `mensaje` | `String` | Contenido de la notificación. |
| `tipo` | `String` | Categoría (INFO, URGENTE). |
| `leido` | `Boolean` | Estado de lectura. |
| `fecha` | `ISODate` | Timestamp de creación. |
