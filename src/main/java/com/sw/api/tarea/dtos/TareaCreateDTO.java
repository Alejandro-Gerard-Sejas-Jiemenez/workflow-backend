package com.sw.api.tarea.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record TareaCreateDTO(
    @NotBlank(message = "El workflowId es obligatorio")
    String workflowId,

    @NotNull(message = "Los datos de la tarea no pueden ser nulos")
    Map<String, Object> datos,
    
    java.util.List<String> documentosUrl
) {}
