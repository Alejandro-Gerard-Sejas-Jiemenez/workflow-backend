package com.sw.api.usuario.repositories;

import com.sw.api.usuario.models.Usuario;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends MongoRepository<Usuario, String> {
    Optional<Usuario> findByEmail(String email);
    Optional<Usuario> findByEmailAndActivoTrue(String email);
    java.util.List<Usuario> findAllByActivoTrue();
    java.util.List<Usuario> findAllByActivoFalse();

}