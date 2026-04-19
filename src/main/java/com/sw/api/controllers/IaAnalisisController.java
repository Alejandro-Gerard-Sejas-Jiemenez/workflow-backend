package com.sw.api.controllers;

import com.sw.api.dtos.IaAnalisisResponseDTO;
import com.sw.api.services.IaAnalisisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tareas")
@RequiredArgsConstructor
public class IaAnalisisController {

    private final IaAnalisisService iaAnalisisService;

    /**
     * CU12: El Funcionario solicita análisis IA de una tarea específica
     * GET /api/tareas/{id}/analizar
     */
    @GetMapping("/{id}/analizar")
    @PreAuthorize("hasAuthority('ROLE_EMPLEADO')")
    public ResponseEntity<IaAnalisisResponseDTO> analizarConIA(@PathVariable String id) {
        return ResponseEntity.ok(iaAnalisisService.analizarTarea(id));
    }
}
