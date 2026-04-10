package com.sw.api.controllers;

import com.sw.api.dtos.PublicacionRequestDTO;
import com.sw.api.dtos.PublicacionResponseDTO;
import com.sw.api.services.PublicacionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/publicaciones")
public class PublicacionController {

    private final PublicacionService publicacionService;

    public PublicacionController(PublicacionService publicacionService) {
        this.publicacionService = publicacionService;
    }

    @PostMapping
    public ResponseEntity<PublicacionResponseDTO> crear(@RequestBody PublicacionRequestDTO dto) {
        PublicacionResponseDTO response = publicacionService.crear(dto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<PublicacionResponseDTO>> obtenerTodas() {
        return ResponseEntity.ok(publicacionService.obtenerTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PublicacionResponseDTO> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(publicacionService.obtenerPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PublicacionResponseDTO> actualizar(@PathVariable UUID id, @RequestBody PublicacionRequestDTO dto) {
        return ResponseEntity.ok(publicacionService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        publicacionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
