package com.sw.api.usuario.controllers;

import com.sw.api.usuario.dtos.RolResponseDTO;
import com.sw.api.usuario.services.RolService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RolController {

    private final RolService rolService;

    @GetMapping

    public ResponseEntity<List<RolResponseDTO>> obtenerRoles() {
        return ResponseEntity.ok(rolService.obtenerTodos());
    }
}
