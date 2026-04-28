package com.sw.api.repositories;

import com.sw.api.models.Tarea;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TareaRepository extends MongoRepository<Tarea, String> {
    List<Tarea> findByWorkflowId(String workflowId);
    
    @Query("{ 'asignadoA' : { $regex: '^?0$', $options: 'i' } }")
    List<Tarea> findByAsignadoA(String asignadoA);
    
    List<Tarea> findBySolicitanteId(String solicitanteId);
    
    List<Tarea> findByEstado(String estado);
}
