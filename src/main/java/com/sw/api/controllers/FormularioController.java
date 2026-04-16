package com.sw.api.controllers;

import com.sw.api.models.Formulario;
import com.sw.api.services.FormularioService;
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

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EMPLEADO')")
    public ResponseEntity<List<Formulario>> obtenerTodos() {
        return ResponseEntity.ok(formularioService.obtenerTodos());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EMPLEADO', 'ROLE_CLIENTE')")
    public ResponseEntity<Formulario> obtenerPorId(@PathVariable String id) {
        return ResponseEntity.ok(formularioService.obtenerPorId(id));
    }
}
