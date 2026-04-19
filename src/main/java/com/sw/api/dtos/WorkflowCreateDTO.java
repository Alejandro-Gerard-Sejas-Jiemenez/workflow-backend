package com.sw.api.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record WorkflowCreateDTO(
    @NotBlank(message = "El nombre es obligatorio")
    String nombre,

    @NotBlank(message = "La descripción es obligatoria")
    String descripcion,

    @NotNull(message = "La lista de pasos no puede ser nula")
    @Size(min = 1, message = "Debe haber al menos un paso en el workflow")
    @Valid
    List<PasoCreateDTO> pasos,

    @NotNull(message = "La lista de reglas no puede ser nula")
    @Valid
    List<ReglaCreateDTO> reglas
) {
    public record PasoCreateDTO(
        @NotBlank(message = "El nombre del paso es obligatorio")
        String nombre,

        @NotNull(message = "El orden es obligatorio")
        Integer orden,

        @NotBlank(message = "El departamento es obligatorio")
        String departamento,

        @NotBlank(message = "El formularioId es obligatorio")
        String formularioId
    ) {}

    public record ReglaCreateDTO(
        @NotBlank(message = "La condición es obligatoria")
        String condicion,

        @NotBlank(message = "La acción es obligatoria")
        String accion
    ) {}
}
