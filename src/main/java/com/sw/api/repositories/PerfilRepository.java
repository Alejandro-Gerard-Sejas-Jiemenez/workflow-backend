package com.sw.api.repositories;

import com.sw.api.models.Perfil;
import com.sw.api.models.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PerfilRepository extends JpaRepository<Perfil, UUID> {
    
    Optional<Perfil> findByUsuario(Usuario usuario);
    
}