package com.sw.api.models;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "perfil")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Perfil {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = true)
    private String apellido;

    private String fotoUrl;

    // Relación 1 a 1 Unidireccional (Perfil sabe de Usuario, Usuario no sabe de
    // Perfil)
    @OneToOne
    @JoinColumn(name = "id_usuario", nullable = false, unique = true)
    private Usuario usuario;

    // Relación N a 1 Unidireccional
    @ManyToOne
    @JoinColumn(name = "id_tipo_perfil", nullable = false)
    private TipoPerfil tipoPerfil;
}