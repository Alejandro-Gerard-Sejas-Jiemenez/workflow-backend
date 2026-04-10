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

## 🔄 2. Workflows y Tareas (Próximamente)

El sistema ya cuenta con los modelos en MongoDB listos para implementar los siguientes flujos:

### Flujo de Tarea (Diseño)
- **Datos Dinámicos**: El campo `datos` permite guardar cualquier estructura de formulario.
- **Historial**: Cada acción queda registrada con usuario, fecha y detalle.

| Entidad | Propósito |
| :--- | :--- |
| **Workflow** | Define los pasos y reglas de un proceso. |
| **Formulario** | Define los campos (input) que requiere cada paso. |
| **Tarea** | Es una instancia viva de un workflow asignada a un usuario. |

---

## 🛠️ Cómo Probar la API
Usa **Swagger UI** para probar las rutas sin herramientas externas:

Acceso:
```text
http://localhost:8080/swagger-ui/index.html
```

> [!IMPORTANT]
> Para usar las rutas protegidas, primero haz **Login**, copia el token y pégalo en el botón **Authorize** de Swagger con el formato: `Bearer TU_TOKEN_AQUI`.
