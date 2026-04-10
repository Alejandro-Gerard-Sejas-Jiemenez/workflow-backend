package com.sw.api.repositories;

import com.sw.api.models.TipoPerfil;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface TipoPerfilRepository extends JpaRepository<TipoPerfil, UUID> {
    Optional<TipoPerfil> findByNombre(String nombre);
}