package com.sw.api.controllers;

import com.sw.api.dtos.WorkflowCreateDTO;
import com.sw.api.dtos.WorkflowCollaboratorAddDTO;
import com.sw.api.dtos.WorkflowCollaboratorDTO;
import com.sw.api.dtos.WorkflowPasosUpdateDTO;
import com.sw.api.dtos.WorkflowReglasUpdateDTO;
import com.sw.api.dtos.WorkflowDiagramUpdateDTO;
import com.sw.api.dtos.WorkflowEstadoUpdateDTO;
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

    @PutMapping("/{id}/pasos")
    @PreAuthorize("hasAuthority('ROLE_DESIGNER')")
    public ResponseEntity<WorkflowResponseDTO> actualizarPasos(
            @PathVariable String id, 
            @Valid @RequestBody WorkflowPasosUpdateDTO dto) {
        return ResponseEntity.ok(workflowService.actualizarPasos(id, dto));
    }

    @PutMapping("/{id}/reglas")
    @PreAuthorize("hasAuthority('ROLE_DESIGNER')")
    public ResponseEntity<WorkflowResponseDTO> actualizarReglas(
            @PathVariable String id, 
            @Valid @RequestBody WorkflowReglasUpdateDTO dto) {
        return ResponseEntity.ok(workflowService.actualizarReglas(id, dto));
    }

    @PutMapping("/{id}/diagrama")
    @PreAuthorize("hasAuthority('ROLE_DESIGNER')")
    public ResponseEntity<WorkflowResponseDTO> actualizarDiagrama(
            @PathVariable String id, 
            @Valid @RequestBody WorkflowDiagramUpdateDTO dto) {
        return ResponseEntity.ok(workflowService.actualizarDiagrama(id, dto));
    }

    @PutMapping("/{id}/estado")
    @PreAuthorize("hasAnyAuthority('ROLE_DESIGNER', 'ROLE_ADMIN')")
    public ResponseEntity<WorkflowResponseDTO> actualizarEstado(
            @PathVariable String id, 
            @Valid @RequestBody WorkflowEstadoUpdateDTO dto) {
        return ResponseEntity.ok(workflowService.actualizarEstado(id, dto));
    }

    @GetMapping("/{id}/collaborators")
    @PreAuthorize("hasAuthority('ROLE_DESIGNER')")
    public ResponseEntity<List<WorkflowCollaboratorDTO>> obtenerColaboradores(@PathVariable String id) {
        return ResponseEntity.ok(workflowService.obtenerColaboradores(id));
    }

    @PostMapping("/{id}/collaborators")
    @PreAuthorize("hasAuthority('ROLE_DESIGNER')")
    public ResponseEntity<WorkflowCollaboratorDTO> agregarColaborador(
            @PathVariable String id,
            @Valid @RequestBody WorkflowCollaboratorAddDTO dto) {
        return ResponseEntity.ok(workflowService.agregarColaborador(id, dto));
    }

    @DeleteMapping("/{id}/collaborators/{userId}")
    @PreAuthorize("hasAuthority('ROLE_DESIGNER')")
    public ResponseEntity<Void> quitarColaborador(@PathVariable String id, @PathVariable String userId) {
        workflowService.quitarColaborador(id, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_DESIGNER')")
    public ResponseEntity<Void> eliminarWorkflow(@PathVariable String id) {
        workflowService.eliminarWorkflow(id);
        return ResponseEntity.noContent().build();
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
