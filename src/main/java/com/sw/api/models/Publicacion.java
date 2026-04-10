package com.sw.api.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "publicacion")
public class Publicacion extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne
    @JoinColumn(name = "id_inmueble", nullable = false)
    private Inmueble inmueble;

    @Column(nullable = false)
    private String titulo;

    @Column(name = "descripcion_general", columnDefinition = "TEXT")
    private String descripcionGeneral;

    @Column(name = "tipo_transaccion", nullable = false)
    private String tipoTransaccion;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal precio;

    @Column(nullable = false, length = 10)
    private String moneda = "USD";

    @Column(name = "estado_publicacion", nullable = false)
    private String estadoPublicacion = "ACTIVO";

    @Column(name = "fecha_publicacion")
    private LocalDateTime fechaPublicacion;
    
    @OneToMany(mappedBy = "publicacion", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<ImagenPublicacion> imagenes = new java.util.ArrayList<>();

    public void addImagen(ImagenPublicacion imagen) {
        imagenes.add(imagen);
        imagen.setPublicacion(this);
    }

    public void removeImagen(ImagenPublicacion imagen) {
        imagenes.remove(imagen);
        imagen.setPublicacion(null);
    }

    @PrePersist
    public void prePersistPublicacion() {
        if (this.fechaPublicacion == null) {
            this.fechaPublicacion = LocalDateTime.now();
        }
    }
}
