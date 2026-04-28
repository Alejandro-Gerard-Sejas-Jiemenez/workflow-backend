package com.sw.api.dtos;

import java.util.List;
import java.util.Map;
import com.sw.api.models.Tarea;

public record TareaResponseDTO(
    String id,
    String workflowId,
    String estado,
    Integer pasoActual,
    String asignadoA,
    String prioridad,
    Map<String, Object> datos,
    List<String> documentosUrl,
    List<Tarea.Historial> historial,
    List<Tarea.Comentario> comentarios,
    com.sw.api.models.Formulario formulario
) {}
