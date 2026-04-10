package com.sw.api.dtos;

import java.math.BigDecimal;

public record InmuebleRequestDTO(
    String tipoInmueble,
    BigDecimal areaTerreno,
    BigDecimal areaConstruida,
    Integer habitaciones,
    Integer banos,
    Integer garajes,
    Integer antiguedadAnios,
    UbicacionDTO ubicacion
) {}
