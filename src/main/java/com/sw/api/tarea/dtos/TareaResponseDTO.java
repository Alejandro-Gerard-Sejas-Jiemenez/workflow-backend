package com.sw.api.tarea.dtos;

import com.sw.api.formulario.models.Formulario;

import java.util.List;
import java.util.Map;
import com.sw.api.tarea.models.Tarea;

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
    com.sw.api.formulario.models.Formulario formulario
) {}
