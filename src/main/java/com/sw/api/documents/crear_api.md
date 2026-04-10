# 🏢 Guía: Crear Endpoint (POST) para Inmueble - Spring Boot + Flyway + Swagger

Este documento explica cómo crear **un endpoint para registrar un inmueble** dentro de un sistema inmobiliario.

---

# 🧱 1. Base de Datos (Flyway)

**Rol:** Define la estructura física de la base de datos de forma versionada.

**Importancia:**

* Permite mantener control de cambios en la BD
* Evita inconsistencias entre entornos (dev, QA, prod)
* Garantiza que todos tengan la misma estructura

Ubicación:

```
src/main/resources/db/migration
```

Archivo:

```
V1__create_table_inmueble.sql
```

```sql
CREATE TABLE inmueble (
    id_inmueble BIGSERIAL PRIMARY KEY,
    tipo_inmueble VARCHAR(50) NOT NULL,
    area_terreno DECIMAL(10,2),
    area_construida DECIMAL(10,2),
    habitaciones INT,
    banos INT,
    garajes INT,
    antiguedad_anios INT
);
```

✔️ Se ejecuta automáticamente al iniciar la aplicación.

---

# 🧩 2. Model

**Rol:** Representa la estructura de datos dentro de la aplicación.

**Importancia:**

* Es el objeto que viaja entre capas (Repository ↔ Service)
* Modela la tabla de la BD en código Java

```java
public class Inmueble {

    private Long id;
    private String tipoInmueble;
    private Double areaTerreno;
    private Double areaConstruida;
    private Integer habitaciones;
    private Integer banos;
    private Integer garajes;
    private Integer antiguedadAnios;
}
```

---

# 📄 3. DTOs

**Rol:** Definen los datos que entran y salen del sistema.

**Importancia:**

* Evitan exponer directamente el modelo interno
* Permiten controlar qué datos recibe y devuelve la API

## Request DTO

```java
public class InmuebleRequestDTO {
    private String tipoInmueble;
    private Double areaTerreno;
    private Double areaConstruida;
    private Integer habitaciones;
    private Integer banos;
    private Integer garajes;
    private Integer antiguedadAnios;
}
```

## Response DTO

```java
public class InmuebleResponseDTO {
    private Long id;
    private String tipoInmueble;
    private Double areaTerreno;
    private Double areaConstruida;
    private Integer habitaciones;
    private Integer banos;
    private Integer garajes;
    private Integer antiguedadAnios;
}
```

---

# 📦 4. Repository

**Rol:** Capa de acceso a datos.

**Importancia:**

* Se encarga de interactuar con la base de datos
* Evita escribir SQL manual para operaciones básicas
* Permite usar métodos CRUD automáticamente

```java
@Repository
public interface InmuebleRepository extends JpaRepository<Inmueble, Long> {
}
```

---

# 🧠 5. Service

**Rol:** Contiene la lógica de negocio.

**Importancia:**

* Centraliza reglas del sistema
* Orquesta la conversión DTO ↔ Model
* Evita lógica en el controller

```java
@Service
public class InmuebleService {

    private final InmuebleRepository repository;

    public InmuebleService(InmuebleRepository repository) {
        this.repository = repository;
    }

    public InmuebleResponseDTO crear(InmuebleRequestDTO dto) {

        // DTO → Model
        Inmueble inmueble = new Inmueble();
        inmueble.setTipoInmueble(dto.getTipoInmueble());
        inmueble.setAreaTerreno(dto.getAreaTerreno());
        inmueble.setAreaConstruida(dto.getAreaConstruida());
        inmueble.setHabitaciones(dto.getHabitaciones());
        inmueble.setBanos(dto.getBanos());
        inmueble.setGarajes(dto.getGarajes());
        inmueble.setAntiguedadAnios(dto.getAntiguedadAnios());

        Inmueble guardado = repository.save(inmueble);

        // Model → Response DTO
        InmuebleResponseDTO response = new InmuebleResponseDTO();
        response.setId(guardado.getId());
        response.setTipoInmueble(guardado.getTipoInmueble());
        response.setAreaTerreno(guardado.getAreaTerreno());
        response.setAreaConstruida(guardado.getAreaConstruida());
        response.setHabitaciones(guardado.getHabitaciones());
        response.setBanos(guardado.getBanos());
        response.setGarajes(guardado.getGarajes());
        response.setAntiguedadAnios(guardado.getAntiguedadAnios());

        return response;
    }
}
```

---

# 🌐 6. Controller (Endpoint)

**Rol:** Expone la API al exterior.

**Importancia:**

* Recibe peticiones HTTP
* Valida/transforma inputs básicos
* Delega la lógica al Service

```java
@RestController
@RequestMapping("/api/inmuebles")
public class InmuebleController {

    private final InmuebleService service;

    public InmuebleController(InmuebleService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<InmuebleResponseDTO> crear(@RequestBody InmuebleRequestDTO dto) {
        return ResponseEntity.ok(service.crear(dto));
    }
}
```

---

# 🧪 7. Probar con Swagger

**Rol:** Herramienta de documentación y pruebas de la API.

**Importancia:**

* Permite probar endpoints sin herramientas externas
* Facilita el entendimiento del contrato de la API

Acceso:

```
http://localhost:8080/swagger-ui/index.html
```

## Endpoint

```
POST /api/inmuebles
```

## Body de ejemplo

```json
{
  "tipoInmueble": "Casa",
  "areaTerreno": 250.5,
  "areaConstruida": 180.0,
  "habitaciones": 4,
  "banos": 3,
  "garajes": 2,
  "antiguedadAnios": 5
}
```

---

# 🧱 Estructura esperada

```
controller/
service/
repository/
dto/
model/
```


