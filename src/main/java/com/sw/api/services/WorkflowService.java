package com.sw.api.services;

import com.sw.api.models.Workflow;
import com.sw.api.dtos.WorkflowCreateDTO;
import com.sw.api.dtos.WorkflowResponseDTO;
import com.sw.api.repositories.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final WorkflowRepository workflowRepository;

    public WorkflowResponseDTO crear(WorkflowCreateDTO dto) {
        Workflow workflow = new Workflow();
        workflow.setNombre(dto.nombre());
        workflow.setDescripcion(dto.descripcion());
        
        List<Workflow.Paso> pasos = dto.pasos().stream().map(p -> 
            new Workflow.Paso(p.nombre(), p.orden(), p.departamento(), p.formularioId())
        ).collect(Collectors.toList());
        workflow.setPasos(pasos);

        List<Workflow.Regla> reglas = dto.reglas().stream().map(r -> 
            new Workflow.Regla(r.condicion(), r.accion())
        ).collect(Collectors.toList());
        workflow.setReglas(reglas);

        return mapToDTO(workflowRepository.save(workflow));
    }

    public List<WorkflowResponseDTO> obtenerTodos() {
        return workflowRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public WorkflowResponseDTO obtenerPorId(String id) {
        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workflow no encontrado con ID: " + id));
        return mapToDTO(workflow);
    }
    
    private WorkflowResponseDTO mapToDTO(Workflow w) {
        return new WorkflowResponseDTO(
            w.getId(),
            w.getNombre(),
            w.getDescripcion(),
            w.getPasos(),
            w.getReglas()
        );
    }
}
