package com.sw.api.tarea.dtos;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record TareaAvanceDTO(
    Map<String, Object> nuevosDatos,
    
    @NotBlank(message = "La acción es obligatoria para el historial")
    String accionUsuario,
    
    String detalle
) {}
