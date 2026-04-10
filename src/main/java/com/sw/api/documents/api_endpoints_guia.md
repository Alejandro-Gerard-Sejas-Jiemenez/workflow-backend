# 📚 Guía de Endpoints (API) - Inmuebles y Publicaciones

Esta guía detalla cómo utilizar las rutas REST creadas para gestionar inmuebles y sus publicaciones dentro de la plataforma.

---

## 🏠 1. Inmuebles (`/api/inmuebles`)

Este grupo de rutas administra la existencia física de la propiedad y su ubicación geográfica de manera transaccional.

### Crear Inmueble y Ubicación
- **Método:** `POST`
- **Ruta:** `/api/inmuebles`
- **Descripción:** Crea un inmueble; opcionalmente puede recibir el bloque `"ubicacion"` para crear e interconectar la ubicación geográfica en la misma transacción.

**Ejemplo Body (Request):**
```json
{
  "tipoInmueble": "Casa",
  "areaTerreno": 150.5,
  "areaConstruida": 120.0,
  "habitaciones": 3,
  "banos": 2,
  "garajes": 1,
  "antiguedadAnios": 5,
  "ubicacion": {
    "ciudad": "Santa Cruz",
    "zonaBarrios": "Equipetrol",
    "direccionExacta": "Calle los Piños #123",
    "latitud": "-17.7833",
    "longitud": "-63.1821"
  }
}
```

### Obtener todos los Inmuebles
- **Método:** `GET`
- **Ruta:** `/api/inmuebles`
- **Respuesta:** Devuelve un Array JSON `[ { ... } ]` con todos los Inmuebles (incluyendo sus direcciones atadas).

### Obtener un Inmueble 
- **Método:** `GET`
- **Ruta:** `/api/inmuebles/{id}`
- **Ejemplo Ruta:** `/api/inmuebles/69c09764-3fad-4dbc-9b09-13953d65f68d`

### Actualizar Inmueble / Cambiar ubicación
- **Método:** `PUT`
- **Ruta:** `/api/inmuebles/{id}`
- **Descripción:** Envía únicamente los campos que desees modificar. Si envías el bloque `ubicacion`, se actualizará la ubicación incrustada.

### Eliminar Inmueble (Soft Delete)
- **Método:** `DELETE`
- **Ruta:** `/api/inmuebles/{id}`
- **Descripción:** Elimina lógicamente el inmueble y, en cascada, su ubicación.

---

## 📢 2. Publicaciones (`/api/publicaciones`)

Este grupo enlaza a un `Usuario` (agente/vendedor) con un `Inmueble`, gestionando el precio, estado e hipervínculos de sus imágenes.

### Crear Publicación (y anexar fotos)
- **Método:** `POST`
- **Ruta:** `/api/publicaciones`
- **Condición:** Debes haber creado un *Usuario* y un *Inmueble* previamente para obtener sus UUIDs.

**Ejemplo Body (Request):**
```json
{
  "idUsuario": "f7fff9ae-55a3-43e4-b287-2bc0a2c03955",
  "idInmueble": "69c09764-3fad-4dbc-9b09-13953d65f68d",
  "titulo": "Casa facherita en Urubó",
  "descripcionGeneral": "Casa bien facherita en el urubo compre ya",
  "tipoTransaccion": "Venta",
  "precio": 150000.00,
  "moneda": "USD",
  "estadoPublicacion": "ACTIVO",
  "imagenesUrls": [
    "https://ejemplo.com/fotocasa1.jpg",
    "https://ejemplo.com/fotocasa2.jpg"
  ]
}
```
*Nota: La primera URL del array `"imagenesUrls"` quedará automáticamente marcada por el API como la Portada (`"esPortada": true`).*

### Obtener Todas las Publicaciones 
- **Método:** `GET`
- **Ruta:** `/api/publicaciones`
- **Respuesta:** Devuelve la lista de publicaciones para que Frontend construya las tarjetas, con el inmueble y galería fotográfica completamente extraídos e incrustados.

### Obtener detalle de Publicación
- **Método:** `GET`
- **Ruta:** `/api/publicaciones/{id}`

### Actualizar Publicación
- **Método:** `PUT`
- **Ruta:** `/api/publicaciones/{id}`
- **Aviso:** Si envías un nuevo grupo de `"imagenesUrls"`, se sobreescribirá la galería entera antigua y guardará la nueva colección por transaccionalidad total.

### Eliminar Publicación
- **Método:** `DELETE`
- **Ruta:** `/api/publicaciones/{id}`
- **Aviso:** Esta acción borrará todas las relaciones fotográficas atadas a ella, pero **NO** borrará el inmueble físico (de esta manera la plataforma conserva el registro del inmueble).
