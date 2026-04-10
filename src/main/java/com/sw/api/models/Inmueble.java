package com.sw.api.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "inmueble")
public class Inmueble extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tipo_inmueble", nullable = false)
    private String tipoInmueble;

    @Column(name = "area_terreno")
    private BigDecimal areaTerreno;

    @Column(name = "area_construida")
    private BigDecimal areaConstruida;

    private Integer habitaciones;
    
    private Integer banos;
    
    private Integer garajes;

    @Column(name = "antiguedad_anios")
    private Integer antiguedadAnios;

    @OneToOne(mappedBy = "inmueble", cascade = CascadeType.ALL, orphanRemoval = true)
    private Ubicacion ubicacion;

    public void setUbicacion(Ubicacion ubicacion) {
        if (ubicacion != null) {
            ubicacion.setInmueble(this);
        }
        this.ubicacion = ubicacion;
    }
}
