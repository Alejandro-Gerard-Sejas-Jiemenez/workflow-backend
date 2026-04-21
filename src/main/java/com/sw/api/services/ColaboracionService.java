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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ColaboracionService {

    private final TareaRepository tareaRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionRepository notificacionRepository;

    /** Agregar comentario a una tarea con soporte de menciones y archivos */
    public ComentarioResponseDTO agregarComentario(String tareaId, ComentarioCreateDTO dto, String username) {
        Tarea tarea = tareaRepository.findById(tareaId)
                .orElseThrow(() -> new RuntimeException("Tarea no encontrada con ID: " + tareaId));

        Usuario autor = usuarioRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        List<String> menciones = normalizarMenciones(dto.menciones(), autor.getId());
        LocalDateTime fechaComentario = LocalDateTime.now();

        Tarea.Comentario comentario = new Tarea.Comentario(
                UUID.randomUUID().toString(),
                autor.getId(),
                dto.contenido(),
                menciones,
                dto.archivoUrl(),
                fechaComentario
        );

        if (tarea.getComentarios() == null) {
            tarea.setComentarios(new ArrayList<>());
        }
        tarea.getComentarios().add(comentario);

        if (tarea.getHistorial() == null) {
            tarea.setHistorial(new ArrayList<>());
        }
        tarea.getHistorial().add(new Tarea.Historial(
                autor.getId(),
                "COMENTARIO_TAREA",
                "Se agrego un comentario colaborativo a la tarea",
                fechaComentario
        ));

        tareaRepository.save(tarea);

        menciones.forEach(mencionadoId -> {
            Notificacion notif = new Notificacion();
            notif.setUsuarioId(mencionadoId);
            notif.setMensaje(autor.getNombre() + " te menciono en la tarea " + tareaId);
            notif.setTipo("MENCION_TAREA");
            notif.setLeido(false);
            notif.setFecha(fechaComentario);
            notificacionRepository.save(notif);
        });

        return mapComentarioToDTO(comentario);
    }

    /** Listar todos los comentarios de una tarea */
    public List<ComentarioResponseDTO> listarComentarios(String tareaId) {
        Tarea tarea = tareaRepository.findById(tareaId)
                .orElseThrow(() -> new RuntimeException("Tarea no encontrada con ID: " + tareaId));

        if (tarea.getComentarios() == null) {
            return new ArrayList<>();
        }

        return tarea.getComentarios().stream()
                .map(this::mapComentarioToDTO)
                .collect(Collectors.toList());
    }

    private ComentarioResponseDTO mapComentarioToDTO(Tarea.Comentario comentario) {
        return new ComentarioResponseDTO(
                comentario.getId(),
                comentario.getUsuarioId(),
                comentario.getContenido(),
                comentario.getMenciones(),
                comentario.getArchivoUrl(),
                comentario.getFecha()
        );
    }

    private List<String> normalizarMenciones(List<String> menciones, String autorId) {
        if (menciones == null || menciones.isEmpty()) {
            return new ArrayList<>();
        }

        return new LinkedHashSet<>(menciones).stream()
                .filter(mencionadoId -> !mencionadoId.equals(autorId))
                .peek(mencionadoId -> {
                    if (!usuarioRepository.existsById(mencionadoId)) {
                        throw new RuntimeException("Usuario mencionado no encontrado: " + mencionadoId);
                    }
                })
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
