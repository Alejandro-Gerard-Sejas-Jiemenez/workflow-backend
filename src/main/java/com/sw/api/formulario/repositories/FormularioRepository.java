package com.sw.api.formulario.repositories;

import com.sw.api.formulario.models.Formulario;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FormularioRepository extends MongoRepository<Formulario, String> {
}
