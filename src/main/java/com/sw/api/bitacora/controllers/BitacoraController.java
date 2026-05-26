package com.sw.api.bitacora.controllers;

import com.sw.api.bitacora.dtos.BitacoraDTO;
import com.sw.api.bitacora.services.BitacoraService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bitacora")
@RequiredArgsConstructor
public class BitacoraController {

    private final BitacoraService bitacoraService;

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<BitacoraDTO>> obtenerLogs() {
        return ResponseEntity.ok(bitacoraService.obtenerLogs());
    }
}
