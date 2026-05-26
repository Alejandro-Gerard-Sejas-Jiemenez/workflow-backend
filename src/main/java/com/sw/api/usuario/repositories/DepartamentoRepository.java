package com.sw.api.usuario.repositories;

import com.sw.api.usuario.models.Departamento;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DepartamentoRepository extends MongoRepository<Departamento, String> {
    Optional<Departamento> findByNombre(String nombre);
}
