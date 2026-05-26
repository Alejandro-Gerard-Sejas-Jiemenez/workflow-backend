package com.sw.api.usuario.dtos;

import java.util.List;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UsuarioCreateDTO(
        @NotBlank(message = "El nombre es obligatorio") String nombre,

        @NotBlank(message = "El email es obligatorio") @Email(message = "Formato de email inválido") String email,

        @NotBlank(message = "La contraseña es obligatoria") @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres") String password,

        List<String> departamentos,

        @NotBlank(message = "El ID del rol es obligatorio") String rolId) {
}
