package com.sw.api.workflow.dtos;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record ComentarioCreateDTO(
    @NotBlank(message = "El contenido del comentario es obligatorio")
    String contenido,

    List<String> menciones,   // Lista de usuarioIds mencionados (opcional)
    String archivoUrl         // URL de Cloudinary si se subió un archivo (opcional)
) {}
