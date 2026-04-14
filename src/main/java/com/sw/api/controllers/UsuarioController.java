package com.sw.api.controllers;

import com.sw.api.dtos.UsuarioResponseDTO;
import com.sw.api.dtos.UsuarioUpdateDTO;
import com.sw.api.services.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    public ResponseEntity<List<UsuarioResponseDTO>> obtenerUsuarios() {
        return ResponseEntity.ok(usuarioService.obtenerTodos());
    }

    @GetMapping("/inactivos")
    public ResponseEntity<List<UsuarioResponseDTO>> obtenerUsuariosInactivos() {
        return ResponseEntity.ok(usuarioService.obtenerInactivos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> obtenerUsuario(@PathVariable String id) {
        return ResponseEntity.ok(usuarioService.obtenerPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> actualizarUsuario(
            @PathVariable String id, 
            @RequestBody UsuarioUpdateDTO dto) {
        return ResponseEntity.ok(usuarioService.actualizar(id, dto));
    }

    @PutMapping("/{id}/restaurar")
    public ResponseEntity<Void> restaurarUsuario(@PathVariable String id) {
        usuarioService.restaurar(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarUsuario(@PathVariable String id) {
        usuarioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
