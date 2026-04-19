package com.sw.api.controllers;

import com.sw.api.dtos.WorkflowCreateDTO;
import com.sw.api.dtos.WorkflowResponseDTO;
import com.sw.api.services.WorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_DESIGNER')")
    public ResponseEntity<WorkflowResponseDTO> crearWorkflow(@Valid @RequestBody WorkflowCreateDTO dto) {
        return ResponseEntity.ok(workflowService.crear(dto));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_EMPLEADO', 'ROLE_CLIENTE', 'ROLE_DESIGNER')")
    public ResponseEntity<List<WorkflowResponseDTO>> obtenerTodos() {
        return ResponseEntity.ok(workflowService.obtenerTodos());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_EMPLEADO', 'ROLE_CLIENTE', 'ROLE_DESIGNER')")
    public ResponseEntity<WorkflowResponseDTO> obtenerPorId(@PathVariable String id) {
        return ResponseEntity.ok(workflowService.obtenerPorId(id));
    }
}
