package com.sw.api.usuario.dtos;

import java.util.List;

public record UsuarioUpdateDTO(
        String nombre,
        List<String> departamentos,
        String rolId) {
}
