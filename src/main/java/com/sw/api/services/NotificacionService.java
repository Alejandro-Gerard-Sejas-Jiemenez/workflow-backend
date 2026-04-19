package com.sw.api.services;

import com.sw.api.dtos.NotificacionCreateDTO;
import com.sw.api.dtos.NotificacionResponseDTO;
import com.sw.api.models.Notificacion;
import com.sw.api.repositories.NotificacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;

    /** Admin: enviar notificación manual a un usuario */
    public NotificacionResponseDTO crear(NotificacionCreateDTO dto) {
        Notificacion n = new Notificacion();
        n.setUsuarioId(dto.usuarioId());
        n.setMensaje(dto.mensaje());
        n.setTipo(dto.tipo());
        n.setLeido(false);
        n.setFecha(LocalDateTime.now());
        return mapToDTO(notificacionRepository.save(n));
    }

    /** Admin: listar todas las notificaciones del sistema */
    public List<NotificacionResponseDTO> obtenerTodas() {
        return notificacionRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /** Admin/Empleado/Cliente: listar las notificaciones de un usuario */
    public List<NotificacionResponseDTO> obtenerPorUsuario(String usuarioId) {
        return notificacionRepository.findByUsuarioId(usuarioId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /** Usuario: marcar notificación como leída */
    public NotificacionResponseDTO marcarLeida(String id) {
        Notificacion n = notificacionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notificación no encontrada con ID: " + id));
        n.setLeido(true);
        return mapToDTO(notificacionRepository.save(n));
    }

    /** Admin: eliminar notificación */
    public void eliminar(String id) {
        if (!notificacionRepository.existsById(id)) {
            throw new RuntimeException("Notificación no encontrada con ID: " + id);
        }
        notificacionRepository.deleteById(id);
    }

    private NotificacionResponseDTO mapToDTO(Notificacion n) {
        return new NotificacionResponseDTO(
                n.getId(),
                n.getUsuarioId(),
                n.getMensaje(),
                n.getTipo(),
                n.isLeido(),
                n.getFecha()
        );
    }
}
