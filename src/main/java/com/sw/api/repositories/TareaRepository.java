package com.sw.api.repositories;

import com.sw.api.models.Tarea;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TareaRepository extends MongoRepository<Tarea, String> {
    List<Tarea> findByWorkflowId(String workflowId);
    List<Tarea> findByAsignadoA(String asignadoA);
    List<Tarea> findByEstado(String estado);
}
