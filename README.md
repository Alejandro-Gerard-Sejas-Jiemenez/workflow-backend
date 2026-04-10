# 🚀 SpaceShift Backend - API de Gestión

Este es el núcleo de servicios para el proyecto **SpaceShift**, desarrollado con **Spring Boot 4** y **Java 21**. Implementa una arquitectura robusta de seguridad basada en **JWT** y persistencia de datos con **PostgreSQL**.

---

## 🛠️ Tecnologías y Dependencias Clave

El proyecto utiliza un stack moderno para garantizar escalabilidad y seguridad:

* **Spring Security & JWT:** Gestiona la autenticación híbrida (Tokens en el Body y Cookies `HttpOnly`) para proteger las rutas de la API.
* **Spring Data JPA:** Abstracción para el manejo de la base de datos mediante entidades Java.
* **PostgreSQL Driver:** Conector oficial para la base de datos relacional.
* **Flyway Migration:** Control de versiones para el esquema de la base de datos (asegura que todos tengan las mismas tablas).
* **Lombok:** Biblioteca para reducir el código repetitivo (Getters, Setters, Builders).
* **Springdoc OpenAPI (Swagger):** Documentación interactiva de los endpoints.

---

## 🚀 Guía de Configuración Local

Para que el proyecto funcione en tu máquina, sigue estos pasos:

### 1. Clonar el repositorio
```bash
git clone [https://github.com/Mau8877/SpaceShift-Backend.git](https://github.com/Mau8877/SpaceShift-Backend.git)
cd SpaceShift-Backend
```
### 2. Configurar las Variables de Entorno
Por seguridad, el archivo de configuración real no se sube al repositorio. Debes crear el tuyo propio:

Localiza el archivo src/main/resources/application.properties.example.

Crea una copia en la misma carpeta y cámbiale el nombre a application.properties.

Abre el nuevo archivo y completa tus datos locales de PostgreSQL:

```bash
spring.datasource.url=jdbc:postgresql://localhost:5432/TU_BASE_DE_DATOS
spring.datasource.username=tu_usuario_postgres
spring.datasource.password=tu_password_postgres
```
### 3. Base de Datos
Asegúrate de tener instalado PostgreSQL y de haber creado la base de datos que especificaste en el paso anterior. Flyway se encargará de crear las tablas automáticamente al iniciar la aplicación.

El servidor iniciará por defecto en el puerto 8081.

📖 Documentación de la API (Swagger)
Una vez que el servidor esté corriendo, puedes probar todos los endpoints y ver la documentación interactiva en:

👉 http://localhost:8081/swagger-ui/index.html
