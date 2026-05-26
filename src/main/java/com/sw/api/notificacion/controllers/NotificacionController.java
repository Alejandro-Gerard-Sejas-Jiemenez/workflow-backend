package com.sw.api.notificacion.controllers;

import com.sw.api.notificacion.dtos.NotificacionCreateDTO;
import com.sw.api.notificacion.dtos.NotificacionResponseDTO;
import com.sw.api.notificacion.services.NotificacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionService notificacionService;

    /** Admin: enviar notificación manual */
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<NotificacionResponseDTO> crear(@Valid @RequestBody NotificacionCreateDTO dto) {
        return ResponseEntity.ok(notificacionService.crear(dto));
    }

    /** Admin: ver todas las notificaciones del sistema */
    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<NotificacionResponseDTO>> obtenerTodas() {
        return ResponseEntity.ok(notificacionService.obtenerTodas());
    }

    /** Admin/Empleado/Cliente: ver notificaciones propias por usuarioId */
    @GetMapping("/usuario/{usuarioId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_EMPLEADO', 'ROLE_CLIENTE')")
    public ResponseEntity<List<NotificacionResponseDTO>> obtenerPorUsuario(@PathVariable String usuarioId) {
        return ResponseEntity.ok(notificacionService.obtenerPorUsuario(usuarioId));
    }

    @GetMapping("/me")
    @PreAuthorize("authenticated")
    public ResponseEntity<List<NotificacionResponseDTO>> obtenerMisNotificaciones() {
        return ResponseEntity.ok(notificacionService.obtenerMisNotificaciones());
    }

    /** Cualquier usuario autenticado: marcar como leída */
    @PatchMapping("/{id}/leer")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_EMPLEADO', 'ROLE_CLIENTE', 'ROLE_DESIGNER')")
    public ResponseEntity<NotificacionResponseDTO> marcarLeida(@PathVariable String id) {
        return ResponseEntity.ok(notificacionService.marcarLeida(id));
    }

    /** Admin: eliminar notificación */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        notificacionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
