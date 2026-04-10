package com.sw.api.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "ubicacion")
public class Ubicacion extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "id_inmueble", nullable = false, unique = true)
    private Inmueble inmueble;

    @Column(nullable = false, length = 100)
    private String ciudad;

    @Column(name = "zona_barrios", length = 100)
    private String zonaBarrios;

    @Column(name = "direccion_exacta", length = 255)
    private String direccionExacta;

    @Column(length = 50)
    private String latitud;

    @Column(length = 50)
    private String longitud;
}
