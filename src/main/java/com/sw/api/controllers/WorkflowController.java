package com.sw.api.controllers;

import com.sw.api.models.Workflow;
import com.sw.api.services.WorkflowService;
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

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EMPLEADO', 'ROLE_CLIENTE')")
    public ResponseEntity<List<Workflow>> obtenerTodos() {
        return ResponseEntity.ok(workflowService.obtenerTodos());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EMPLEADO', 'ROLE_CLIENTE')")
    public ResponseEntity<Workflow> obtenerPorId(@PathVariable String id) {
        return ResponseEntity.ok(workflowService.obtenerPorId(id));
    }
}
