# 🚀 SpaceShift Backend - API de Gestión

Este es el núcleo de servicios para el proyecto **SpaceShift**, desarrollado con **Spring Boot 4** y **Java 21**. Implementa una arquitectura moderna NoSQL y seguridad basada en **JWT**.

---

## 🛠️ Tecnologías y Dependencias Clave

El proyecto utiliza un stack moderno para garantizar escalabilidad y seguridad:

* **Spring Security & JWT:** Gestiona la autenticación híbrida (Tokens en el Body y Cookies `HttpOnly`).
* **Spring Data MongoDB:** Persistencia de datos orientada a documentos.
* **Spring Data Redis:** Gestión de cache y blacklist de tokens para seguridad.
* **Docker & Docker Compose:** Orquestación de infraestructura local (DBs).
* **Lombok:** Reducción de código repetitivo.
* **Springdoc OpenAPI (Swagger):** Documentación interactiva de los endpoints.

---

## 🚀 Guía de Configuración Local

### 1. Requisitos Previos
- Docker Desktop instalado y corriendo.
- Java 21+ instalado.

### 2. Infraestructura con Docker 🐳
Este proyecto utiliza Docker para levantar sus dependencias. No necesitas instalar MongoDB ni Redis manualmente.

**Comandos básicos:**
- **Levantar base de datos y cache:**
  ```powershell
  docker-compose up -d
  ```
- **Detener servicios:**
  ```powershell
  docker-compose down
  ```
- **Verificar estado de contenedores:**
  ```powershell
  docker ps
  ```

> [!TIP]
> Los datos de MongoDB persistirán gracias a los **volúmenes** configurados en `docker-compose.yml`. Aunque borres el contenedor, tus datos seguirán seguros en tu disco local.

### 3. Ejecutar el Backend
Una vez que los contenedores estén activos:

```powershell
./mvnw spring-boot:run
```

El servidor iniciará por defecto en el puerto **8081**.

---

## 📖 Documentación de la API (Swagger)
Una vez que el servidor esté corriendo, puedes probar todos los endpoints y ver la documentación interactiva en:

👉 [http://localhost:8081/swagger-ui/index.html](http://localhost:8081/swagger-ui/index.html)
