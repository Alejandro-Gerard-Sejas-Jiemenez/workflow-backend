package com.sw.api.ia.dtos;

import java.util.Map;
import java.util.List;

public record IaAnalisisResponseDTO(
    String tareaId,
    String analisis,
    List<String> recomendaciones,
    Map<String, Object> datosAnalizados
) {}
