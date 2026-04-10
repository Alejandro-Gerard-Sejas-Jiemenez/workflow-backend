package com.sw.api.controllers;

import com.sw.api.dtos.InmuebleDTO;
import com.sw.api.dtos.InmuebleRequestDTO;
import com.sw.api.services.InmuebleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/inmuebles")
public class InmuebleController {

    private final InmuebleService inmuebleService;

    public InmuebleController(InmuebleService inmuebleService) {
        this.inmuebleService = inmuebleService;
    }

    @PostMapping
    public ResponseEntity<InmuebleDTO> crear(@RequestBody InmuebleRequestDTO dto) {
        return new ResponseEntity<>(inmuebleService.crear(dto), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<InmuebleDTO>> obtenerTodos() {
        return ResponseEntity.ok(inmuebleService.obtenerTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<InmuebleDTO> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(inmuebleService.obtenerPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<InmuebleDTO> actualizar(@PathVariable UUID id, @RequestBody InmuebleRequestDTO dto) {
        return ResponseEntity.ok(inmuebleService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        inmuebleService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
