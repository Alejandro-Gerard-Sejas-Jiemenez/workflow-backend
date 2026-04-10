package com.sw.api.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "imagen_publicacion")
public class ImagenPublicacion extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "id_publicacion", nullable = false)
    private Publicacion publicacion;

    @Column(name = "url_image", nullable = false, length = 500)
    private String urlImage;

    @Column(name = "es_portada")
    private Boolean esPortada = false;
}
