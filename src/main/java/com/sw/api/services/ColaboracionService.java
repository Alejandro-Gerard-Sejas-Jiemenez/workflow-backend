package com.sw.api.services;

import com.sw.api.dtos.ComentarioCreateDTO;
import com.sw.api.dtos.ComentarioResponseDTO;
import com.sw.api.models.Notificacion;
import com.sw.api.models.Tarea;
import com.sw.api.models.Usuario;
import com.sw.api.repositories.NotificacionRepository;
import com.sw.api.repositories.TareaRepository;
import com.sw.api.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ColaboracionService {

    private final TareaRepository tareaRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionRepository notificacionRepository;

    /** Agregar comentario a una tarea (con soporte de menciones y archivos) */
    public ComentarioResponseDTO agregarComentario(String tareaId, ComentarioCreateDTO dto, String username) {
        Tarea tarea = tareaRepository.findById(tareaId)
                .orElseThrow(() -> new RuntimeException("Tarea no encontrada con ID: " + tareaId));

        Usuario autor = usuarioRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Construir el comentario embebido
        Tarea.Comentario comentario = new Tarea.Comentario(
                UUID.randomUUID().toString(),
                autor.getId(),
                dto.contenido(),
                dto.menciones() != null ? dto.menciones() : new ArrayList<>(),
                dto.archivoUrl(),
                LocalDateTime.now()
        );

        // Inicializar lista si es el primer comentario
        if (tarea.getComentarios() == null) {
            tarea.setComentarios(new ArrayList<>());
        }
        tarea.getComentarios().add(comentario);
        tareaRepository.save(tarea);

        // Disparar notificaciones a los mencionados
        if (dto.menciones() != null) {
            dto.menciones().forEach(mencionadoId -> {
                Notificacion notif = new Notificacion();
                notif.setUsuarioId(mencionadoId);
                notif.setMensaje(autor.getNombre() + " te mencionó en la tarea " + tareaId);
                notif.setTipo("MENCION");
                notif.setLeido(false);
                notif.setFecha(LocalDateTime.now());
                notificacionRepository.save(notif);
            });
        }

        return mapComentarioToDTO(comentario);
    }

    /** Listar todos los comentarios de una tarea */
    public List<ComentarioResponseDTO> listarComentarios(String tareaId) {
        Tarea tarea = tareaRepository.findById(tareaId)
                .orElseThrow(() -> new RuntimeException("Tarea no encontrada con ID: " + tareaId));

        if (tarea.getComentarios() == null) return new ArrayList<>();

        return tarea.getComentarios().stream()
                .map(this::mapComentarioToDTO)
                .collect(Collectors.toList());
    }

    private ComentarioResponseDTO mapComentarioToDTO(Tarea.Comentario c) {
        return new ComentarioResponseDTO(
                c.getId(),
                c.getUsuarioId(),
                c.getContenido(),
                c.getMenciones(),
                c.getArchivoUrl(),
                c.getFecha()
        );
    }
}
