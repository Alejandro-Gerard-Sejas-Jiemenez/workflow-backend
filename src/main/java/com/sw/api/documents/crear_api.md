# 🚀 Guía: Cómo crear una nueva funcionalidad (CRUD)

Esta guía explica el flujo para crear una nueva entidad en este proyecto utilizando **Spring Boot** y **MongoDB**.

---

## 📄 1. El Modelo (Documento)
A diferencia de SQL, aquí no usamos Flyway ni tablas. Creamos una clase con `@Document`.

**Ubicación:** `src/main/java/com/sw/api/models/`

```java
@Document(collection = "entidades")
@Getter @Setter
public class MiEntidad {
    @Id
    private String id; // Siempre usaremos String para MongoDB (ObjectId)
    private String nombre;
}
```

---

## 🗄️ 2. El Repositorio
Heredamos de `MongoRepository`. Esto nos da automáticamente métodos como `save()`, `findAll()` y `deleteById()`.

**Ubicación:** `src/main/java/com/sw/api/repositories/`

```java
@Repository
public interface MiEntidadRepository extends MongoRepository<MiEntidad, String> {
    // Puedes agregar consultas personalizadas si quieres
    Optional<MiEntidad> findByNombre(String nombre);
}
```

---

## 📦 3. Los DTOs (Request/Response)
Son necesarios para no exponer el modelo interno a la API. Usamos `record` para que sea más limpio.

**Ubicación:** `src/main/java/com/sw/api/dtos/`

```java
public record MiEntidadRequest(String nombre) {}
public record MiEntidadResponse(String id, String nombre) {}
```

---

## 🧠 4. El Servicio
Aquí vive la "lógica". Es donde convertimos el DTO a Modelo y viceversa.

**Ubicación:** `src/main/java/com/sw/api/services/`

```java
@Service
@RequiredArgsConstructor
public class MiEntidadService {
    private final MiEntidadRepository repository;

    public MiEntidadResponse crear(MiEntidadRequest request) {
        MiEntidad entidad = new MiEntidad();
        entidad.setNombre(request.nombre());
        MiEntidad guardado = repository.save(entidad);
        return new MiEntidadResponse(guardado.getId(), guardado.getNombre());
    }
}
```

---

## 🔌 5. El Controlador
Expone el servicio al mundo exterior a través de una URL.

**Ubicación:** `src/main/java/com/sw/api/controllers/`

```java
@RestController
@RequestMapping("/api/mi-entidad")
@RequiredArgsConstructor
public class MiEntidadController {
    private final MiEntidadService service;

    @PostMapping
    public ResponseEntity<MiEntidadResponse> crear(@RequestBody MiEntidadRequest request) {
        return ResponseEntity.ok(service.crear(request));
    }
}
```

---

## 💡 Recordatorios Clave para el Examen

> [!TIP]
> **Schema-less**: En MongoDB no necesitas crear tablas. Si añades un nuevo atributo a tu clase `MiEntidad`, MongoDB lo guardará automáticamente la próxima vez que llames a `save()`.

> [!IMPORTANT]
> **Inyección de Dependencias**: Siempre usa `@RequiredArgsConstructor` de Lombok y marca tus variables como `private final`. Esto es el estándar moderno en Spring Boot.

> [!CAUTION]
> **IDs**: Recuerda que en este proyecto decidimos usar `String` para los IDs. Si usas `Long` o `Integer`, MongoDB te dará errores de tipo.
