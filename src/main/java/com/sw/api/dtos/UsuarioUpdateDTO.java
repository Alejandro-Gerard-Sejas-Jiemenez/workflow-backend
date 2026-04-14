package com.sw.api.dtos;

public record UsuarioUpdateDTO(
    String nombre,
    String apellido,
    String departamento,
    String telefono,
    String rolId
) {}
