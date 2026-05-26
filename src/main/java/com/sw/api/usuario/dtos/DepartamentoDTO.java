package com.sw.api.usuario.dtos;

import jakarta.validation.constraints.NotBlank;

public record DepartamentoDTO(
    String id,
    
    @NotBlank(message = "El nombre no puede estar vacío")
    String nombre,
    
    String descripcion,
    
    String estado
) {}
