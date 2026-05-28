package com.sw.api.formulario.dtos;

import java.util.List;
import com.sw.api.formulario.models.Formulario;

public record FormularioResponseDTO(
    String id,
    String nombre,
    String descripcion,
    List<Formulario.Campo> campos,
    List<Object> reglasVisibilidad,
    boolean allowAttachments,
    String allowedTypes,
    String requiredDocs
) {}
