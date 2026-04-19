package com.sw.api.controllers;

import com.sw.api.dtos.TareaCreateDTO;
import com.sw.api.dtos.TareaAvanceDTO;
import com.sw.api.dtos.TareaResponseDTO;
import com.sw.api.services.TareaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/tareas")
@RequiredArgsConstructor
public class TareaController {

    private final TareaService tareaService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_CLIENTE', 'ROLE_EMPLEADO')")
    public ResponseEntity<TareaResponseDTO> iniciarTarea(
            Principal principal,
            @Valid @RequestBody TareaCreateDTO dto) {
        // principal.getName() devuelve el email que inyectó nuestro JWT Filter
        return ResponseEntity.ok(tareaService.iniciarTarea(dto, principal.getName()));
    }

    @PostMapping("/{id}/gestionar")
    @PreAuthorize("hasAuthority('ROLE_EMPLEADO')")
    public ResponseEntity<TareaResponseDTO> gestionarTarea(
            @PathVariable String id,
            @Valid @RequestBody TareaAvanceDTO dto,
            Principal principal) {
        return ResponseEntity.ok(tareaService.gestionarTarea(id, dto, principal.getName()));
    }
}
