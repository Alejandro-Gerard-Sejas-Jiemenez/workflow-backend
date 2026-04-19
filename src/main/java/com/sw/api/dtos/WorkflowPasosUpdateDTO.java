package com.sw.api.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record WorkflowPasosUpdateDTO(
    @NotNull(message = "La lista de pasos no puede ser nula")
    @Valid
    List<WorkflowCreateDTO.PasoCreateDTO> pasos
) {}
