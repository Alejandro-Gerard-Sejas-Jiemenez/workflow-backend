# Description
Reglas generales de desarrollo para el agente de IA en el proyecto SpaceShift.

# Content
Estas reglas guían el comportamiento general del agente para asegurar consistencia en todo el proyecto de backend.

## 🚀 Principios de Desarrollo
- **Stack:** Java 21 con Spring Boot.
- **Persistencia:** MongoDB (orientado a documentos y embebidos).
- **Seguridad:** Autenticación y autorización mediante JWT.

## 🛠️ Reglas de Comportamiento
1. **Contexto:** Antes de realizar cualquier cambio, siempre analiza los modelos (`models`) y los casos de uso definidos en `backend.md`.
2. **Nomenclatura:** Mantener los nombres de las entidades de negocio en **Español** para alineación con el dominio del examen.
3. **Consistencia:** Respetar la arquitectura actual de carpetas (controllers, services, repositories, security).
4. **Validación:** Asegurar que todos los datos de entrada en los controladores estén validados con anotaciones de Jakarta.
