---
trigger: always_on
---

# Description
Reglas de seguridad y gestión de actores (Admin, Empleado, Cliente) para el backend.

# Content
Este módulo define cómo el agente debe manejar la seguridad y los roles en cada endpoint.

## 👤 Roles y Actores
- **ROLE_ADMIN:** Acceso total a la gestión de Workflows, Usuarios y Formularios.
- **ROLE_EMPLEADO:** Acceso al procesamiento de tareas, consulta de historial y gestión de tareas asignadas.
- **ROLE_CLIENTE:** Acceso limitado para iniciar tareas (workflows específicos) y ver el estado de sus propios procesos.

## 🛡️ Reglas de Implementación de Seguridad
- **@PreAuthorize:** Siempre incluir esta anotación en los métodos de los controladores.
- **Validación de Token:** Garantizar que los endpoints sensibles pasen por el filtro de seguridad JWT.
- **Nombres de Roles:** Usar la nomenclatura estándar de Spring Security (`ROLE_...`).
- **Casos de Uso:**
  - Gestión de Catálogos (User/Workflow/Form): Solo Admin.
  - Avance de Solicitud: Empleado.
  - Inicio de Solicitud: Cliente o Empleado.
