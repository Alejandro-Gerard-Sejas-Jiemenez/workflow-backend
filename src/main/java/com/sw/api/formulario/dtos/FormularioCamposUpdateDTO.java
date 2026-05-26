package com.sw.api.formulario.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record FormularioCamposUpdateDTO(
    @NotNull(message = "La lista de campos no puede ser nula")
    @Valid
    List<FormularioCreateDTO.CampoCreateDTO> campos
) {}
