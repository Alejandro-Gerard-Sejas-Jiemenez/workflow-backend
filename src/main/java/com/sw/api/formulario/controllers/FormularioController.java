package com.sw.api.formulario.controllers;

import com.sw.api.formulario.dtos.FormularioCreateDTO;
import com.sw.api.formulario.dtos.FormularioCamposUpdateDTO;
import com.sw.api.formulario.dtos.FormularioResponseDTO;
import com.sw.api.formulario.services.FormularioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/formularios")
@RequiredArgsConstructor
public class FormularioController {

    private final FormularioService formularioService;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_DESIGNER')")
    public ResponseEntity<FormularioResponseDTO> crearFormulario(@Valid @RequestBody FormularioCreateDTO dto) {
        return ResponseEntity.ok(formularioService.crear(dto));
    }

    @PutMapping("/{id}/campos")
    @PreAuthorize("hasAuthority('ROLE_DESIGNER')")
    public ResponseEntity<FormularioResponseDTO> actualizarCampos(
            @PathVariable String id, 
            @Valid @RequestBody FormularioCamposUpdateDTO dto) {
        return ResponseEntity.ok(formularioService.actualizarCampos(id, dto));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_EMPLEADO', 'ROLE_DESIGNER')")
    public ResponseEntity<List<FormularioResponseDTO>> obtenerTodos() {
        return ResponseEntity.ok(formularioService.obtenerTodos());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_EMPLEADO', 'ROLE_CLIENTE', 'ROLE_DESIGNER')")
    public ResponseEntity<FormularioResponseDTO> obtenerPorId(@PathVariable String id) {
        return ResponseEntity.ok(formularioService.obtenerPorId(id));
    }
}
