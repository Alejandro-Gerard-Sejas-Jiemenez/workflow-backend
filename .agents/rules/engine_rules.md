---
trigger: always_on
---

# Description
Reglas para el motor de workflows, evaluación de lógica SpEL y auditoría.

# Content
Este módulo define cómo el agente debe gestionar el comportamiento dinámico del sistema de workflows.

## 🔄 Motor de Evaluación
- **Spring Expression Language (SpEL):** Todas las condiciones de transición deben ser evaluadas usando SpEL.
- **Acceso a Datos:** En las reglas, los datos se acceden mediante el prefijo `#datos`, por ejemplo: `#datos['monto'] > 1000`.
- **Acciones Disponibles:**
  - `SET_STATE(estado)`
  - `SKIP_TO(paso)`
  - `ASSIGN_TO(usuarioId)`

## 📜 Regla de Auditoría y Historial
- **Registro Obligatorio:** Cualquier actualización en una `Tarea` que implique un cambio de estado, de paso o de asignación debe generar una entrada en la lista de `historial`.
- **Campos del Historial:** asegurar que cada registro contenga:
  - `usuarioId`: Quién realizó la acción.
  - `accion`: Descripción corta (creación, aprobación, rechazo).
  - `detalle`: Descripción del cambio.
  - `fecha`: Marca de tiempo ISODate.

## 🤖 Lógica Avanzada
- **Notificaciones:** Al cambiar de paso, el sistema debe disparar automáticamente la creación de una notificación para el nuevo asignado.
- **Priorización:** Evaluar la prioridad de la tarea según las reglas definidas en el workflow al inicio de cada paso.
