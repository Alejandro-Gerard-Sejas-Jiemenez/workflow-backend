package com.sw.api.models;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "tipos_perfil")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TipoPerfil {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String nombre;
}