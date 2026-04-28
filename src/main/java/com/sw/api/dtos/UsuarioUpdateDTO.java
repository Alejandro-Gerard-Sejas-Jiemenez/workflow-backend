package com.sw.api.dtos;

import java.util.List;

public record UsuarioUpdateDTO(
        String nombre,
        List<String> departamentos,
        String rolId) {
}
