package com.sw.api.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record WorkflowReglasUpdateDTO(
    @NotNull(message = "La lista de reglas no puede ser nula")
    @Valid
    List<WorkflowCreateDTO.ReglaCreateDTO> reglas
) {}
