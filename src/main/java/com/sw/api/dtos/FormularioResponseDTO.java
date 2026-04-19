package com.sw.api.dtos;

import java.util.List;
import com.sw.api.models.Formulario;

public record FormularioResponseDTO(
    String id,
    String nombre,
    List<Formulario.Campo> campos,
    List<Object> reglasVisibilidad
) {}
