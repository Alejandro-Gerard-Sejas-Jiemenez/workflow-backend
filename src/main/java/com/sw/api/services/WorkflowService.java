package com.sw.api.services;

import com.sw.api.models.Workflow;
import com.sw.api.repositories.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final WorkflowRepository workflowRepository;

    public List<Workflow> obtenerTodos() {
        return workflowRepository.findAll();
    }

    public Workflow obtenerPorId(String id) {
        return workflowRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workflow no encontrado con ID: " + id));
    }
    
    // Aquí se podrían agregar métodos para crear, actualizar o eliminar workflows en el futuro
}
