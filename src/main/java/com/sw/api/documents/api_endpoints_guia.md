# 📚 Guía de Endpoints (API) - Autenticación y Workflows

Esta guía detalla las rutas disponibles en la API y la estructura de los objetos que recibe y devuelve.

---

## 🔐 1. Autenticación (`/api/auth`)

Gestión de usuarios, seguridad y tokens JWT.

### Registro de Usuario
- **Método:** `POST`
- **Ruta:** `/api/auth/register`
- **Descripción:** Crea un nuevo usuario y le asigna el rol `ROLE_USER` por defecto.

**Body (Request):**
```json
{
  "email": "juan@gmail.com",
  "password": "mi_password_seguro",
  "nombre": "Juan",
  "apellido": "Pérez",
  "departamento": "ventas"
}
```

### Iniciar Sesión (Login)
- **Método:** `POST`
- **Ruta:** `/api/auth/login`
- **Respuesta:** Devuelve un Token JWT que debe enviarse en el Header de las siguientes peticiones.

**Body (Request):**
```json
{
  "email": "juan@gmail.com",
  "password": "mi_password_seguro"
}
```

**Respuesta (Success):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

---

## 🔄 2. Workflows y Tareas

Gestión de procesos de negocio y sus instancias.

### Listar Workflows
- **Método:** `GET`
- **Ruta:** `/api/workflows`
- **Descripción:** Devuelve todos los procesos definidos (ej. Vacaciones, Compras).

### Ver Detalle de Workflow
- **Método:** `GET`
- **Ruta:** `/api/workflows/{id}`

### Ver Formulario
- **Método:** `GET`
- **Ruta:** `/api/formularios/{id}`
- **Descripción:** Obtiene los campos necesarios para un paso específico del workflow.

---

## 📋 3. Tareas (Próximamente)
Las tareas representan instancias de workflows en ejecución.

---

## 🛠️ Cómo Probar la API
Usa **Swagger UI** para probar las rutas sin herramientas externas:

Acceso:
```text
http://localhost:8081/swagger-ui/index.html
```

> [!IMPORTANT]
> Para usar las rutas de Workflows, primero haz **Login** con el usuario admin (`admin@workflow.com`), copia el token y pégalo en el botón **Authorize** de Swagger con el formato: `Bearer TU_TOKEN_AQUI`.
