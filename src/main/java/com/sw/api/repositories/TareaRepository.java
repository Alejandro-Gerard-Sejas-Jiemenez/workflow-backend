package com.sw.api.repositories;

import com.sw.api.models.Tarea;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TareaRepository extends MongoRepository<Tarea, String> {
}
