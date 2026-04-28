package com.sw.api.controllers;

import com.sw.api.dtos.ComentarioCreateDTO;
import com.sw.api.dtos.ComentarioResponseDTO;
import com.sw.api.dtos.TareaCreateDTO;
import com.sw.api.dtos.TareaAvanceDTO;
import com.sw.api.dtos.TareaResponseDTO;
import com.sw.api.services.ColaboracionService;
import com.sw.api.services.TareaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/tareas")
@RequiredArgsConstructor
public class TareaController {

    private final TareaService tareaService;
    private final ColaboracionService colaboracionService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_CLIENTE', 'ROLE_EMPLEADO')")
    public ResponseEntity<TareaResponseDTO> iniciarTarea(
            Principal principal,
            @Valid @RequestBody TareaCreateDTO dto) {
        // principal.getName() devuelve el email que inyectó nuestro JWT Filter
        return ResponseEntity.ok(tareaService.iniciarTarea(dto, principal.getName()));
    }

    @GetMapping("/mis-tareas")
    @PreAuthorize("hasAnyAuthority('ROLE_EMPLEADO', 'ROLE_CLIENTE')")
    public ResponseEntity<List<TareaResponseDTO>> misTareas(Principal principal) {
        return ResponseEntity.ok(tareaService.listarTareasParaEmpleado(principal.getName()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_EMPLEADO', 'ROLE_CLIENTE', 'ROLE_ADMIN')")
    public ResponseEntity<TareaResponseDTO> obtenerPorId(@PathVariable String id) {
        return ResponseEntity.ok(tareaService.obtenerPorId(id));
    }

    @PostMapping("/{id}/gestionar")
    @PreAuthorize("hasAnyAuthority('ROLE_EMPLEADO', 'EMPLEADO', 'ROLE_ADMIN')")
    public ResponseEntity<TareaResponseDTO> gestionarTarea(
            @PathVariable String id,
            @Valid @RequestBody TareaAvanceDTO dto,
            Principal principal) {
        return ResponseEntity.ok(tareaService.gestionarTarea(id, dto, principal.getName()));
    }

    @PostMapping("/{id}/validar")
    @PreAuthorize("hasAnyAuthority('ROLE_EMPLEADO', 'EMPLEADO', 'ROLE_ADMIN')")
    public ResponseEntity<TareaResponseDTO> validarSolicitud(
            @PathVariable String id,
            @Valid @RequestBody com.sw.api.dtos.ValidarSolicitudRequest dto,
            Principal principal) {
        return ResponseEntity.ok(tareaService.validarSolicitud(id, dto, principal.getName()));
    }

    @PostMapping("/{id}/comentarios")
    @PreAuthorize("hasAnyAuthority('ROLE_EMPLEADO', 'EMPLEADO', 'ROLE_ADMIN', 'ROLE_CLIENTE')")
    public ResponseEntity<ComentarioResponseDTO> agregarComentario(
            @PathVariable String id,
            @Valid @RequestBody ComentarioCreateDTO dto,
            Principal principal) {
        return ResponseEntity.ok(colaboracionService.agregarComentario(id, dto, principal.getName()));
    }

    @GetMapping("/{id}/comentarios")
    @PreAuthorize("hasAuthority('ROLE_EMPLEADO')")
    public ResponseEntity<List<ComentarioResponseDTO>> listarComentarios(@PathVariable String id) {
        return ResponseEntity.ok(colaboracionService.listarComentarios(id));
    }
}
