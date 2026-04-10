package com.sw.api.repositories;

import com.sw.api.models.Formulario;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FormularioRepository extends MongoRepository<Formulario, String> {
}
