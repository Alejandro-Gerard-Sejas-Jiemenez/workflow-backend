# 🏛️ Guía de Arquitectura: Motor de Workflows (MongoDB)

Esta guía explica la estructura modular del proyecto y el papel de cada carpeta en nuestra arquitectura basada en **Spring Boot** y **MongoDB**.

---

## 📂 Árbol de Directorios (Base)

```text
src/main/java/com/sw/api/
├── ⚙️ config/          # Configuraciones globales y Beans
├── 🔌 controllers/     # Endpoints REST (Entrada)
├── 📦 dtos/            # Objetos de Transferencia de Datos
├── 📄 models/          # Documentos de MongoDB (Entidades)
├── 🗄️ repositories/    # Capa de Acceso a Datos (Consultas)
├── 🛡️ security/        # Filtros JWT y Seguridad
└── 🧠 services/        # Lógica de Negocio (Cerebro)
```

---

## 🛠️ Detalle de Funcionalidades

| Carpeta | Responsabilidad | Ejemplo de Contenido |
| :--- | :--- | :--- |
| **`config`** | Configura el comportamiento de Spring. | `AuditConfig` (para fechas automáticas). |
| **`controllers`** | Expone las rutas URL al exterior. | `AuthController` (Login/Registro). |
| **`dtos`** | Define qué datos entran y salen de la API. | `RegisterRequest`, `AuthResponse`. |
| **`models`** | Define cómo se guardan los datos en Mongo. | `Usuario`, `Workflow`, `Tarea`. |
| **`repositories`** | Realiza las consultas a la base de datos. | `TareaRepository` (find, save, delete). |
| **`security`** | Protege las rutas con JWT y roles. | `JwtService`, `SecurityConfig`. |
| **`services`** | Procesa los datos y aplica las reglas. | `AuthService` (valida contraseñas). |

---

## 🔄 El Flujo de la Información

Para entender cómo interactúan las carpetas, sigue este flujo cuando llega una petición (ej. Crear una Tarea):

1.  **`Controller`**: Recibe el `DTO` con los datos del formulario.
2.  **`Service`**: Valida que el usuario tenga permisos y procesa la lógica.
3.  **`Repository`**: Convierte el `Model` en un documento para MongoDB.
4.  **`Database`**: Guarda el documento en la colección correspondiente.

---

## 💡 ¿Por qué esta Estructura?

> [!TIP]
> **Desacoplamiento**: Si decides cambiar la lógica de una tarea, solo tocas el `Service`. Si cambias la base de datos, solo tocas el `Model` y el `Repository`. El resto del sistema sigue funcionando igual.

> [!IMPORTANT]
> **Seguridad**: Nunca exponemos el `Model` directamente al frontend. Siempre usamos un `DTO` para filtrar qué información queremos mostrar (ej. nunca enviar la contraseña en un DTO).

---
*Esta estructura está diseñada para ser escalable, segura y fácil de mantener para tu examen y futuro desarrollo.*