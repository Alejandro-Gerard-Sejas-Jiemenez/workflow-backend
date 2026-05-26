package com.sw.api.formulario.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

public record FormularioCreateDTO(
    @NotBlank(message = "El nombre del formulario es obligatorio")
    String nombre,

    @NotNull(message = "La lista de campos no puede ser nula")
    @Valid
    List<CampoCreateDTO> campos,

    List<Object> reglasVisibilidad
) {
    public record CampoCreateDTO(
        @NotBlank(message = "El nombre del campo es obligatorio")
        String nombre,

        @NotBlank(message = "El tipo de campo es obligatorio")
        String tipo,

        Boolean requerido,
        
        Object opciones
    ) {
        public boolean isRequerido() {
            return requerido != null ? requerido : false;
        }
    }
}
