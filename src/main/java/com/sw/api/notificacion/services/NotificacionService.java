package com.sw.api.notificacion.services;

import com.sw.api.workflow.models.Workflow;

import org.springframework.beans.factory.annotation.Value;

import com.sw.api.notificacion.dtos.NotificacionCreateDTO;
import com.sw.api.notificacion.dtos.NotificacionResponseDTO;
import com.sw.api.notificacion.models.Notificacion;
import com.sw.api.notificacion.repositories.NotificacionRepository;
import com.sw.api.usuario.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import com.sw.api.usuario.models.Usuario;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificacionService {

    @Value("${workflow.colab.base-url}")
    private String colabBaseUrl;

    private final NotificacionRepository notificacionRepository;
    private final UsuarioRepository usuarioRepository;

    public void generarNotificacionSystem(String usuarioId, String mensaje, String tipo) {
        Notificacion n = new Notificacion();
        n.setUsuarioId(usuarioId);
        n.setMensaje(mensaje);
        n.setTipo(tipo);
        n.setLeido(false);
        n.setFecha(LocalDateTime.now());
        notificacionRepository.save(n);

        // Delegar el envío de Push al servidor de colaboración (8082)
        usuarioRepository.findById(usuarioId).ifPresent(u -> {
            if (u.getFcmToken() != null && !u.getFcmToken().isBlank()) {
                try {
                    org.springframework.web.client.RestClient.create().post()
                        .uri(colabBaseUrl + "/api/internal/collaboration/push-notification")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .body(Map.of(
                            "token", u.getFcmToken(),
                            "title", "Workflow Update",
                            "body", mensaje
                        ))
                        .retrieve()
                        .toBodilessEntity();
                } catch (Exception e) {
                    System.err.println("No se pudo contactar al servidor de colaboración para Push: " + e.getMessage());
                }
            }
        });
    }

    public List<NotificacionResponseDTO> obtenerMisNotificaciones() {
        return obtenerPorUsuario(currentUser().getId());
    }

    private Usuario currentUser() {
        return (Usuario) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    /** Admin: enviar notificación manual a un usuario */
    public NotificacionResponseDTO crear(NotificacionCreateDTO dto) {
        Notificacion n = new Notificacion();
        n.setUsuarioId(dto.usuarioId());
        n.setMensaje(dto.mensaje());
        n.setTipo(dto.tipo());
        n.setLeido(false);
        n.setFecha(LocalDateTime.now());
        
        NotificacionResponseDTO response = mapToDTO(notificacionRepository.save(n));
        
        // Enviar Push también para notificaciones manuales delegando al servidor de colaboración (8082)
        usuarioRepository.findById(dto.usuarioId()).ifPresent(u -> {
            if (u.getFcmToken() != null && !u.getFcmToken().isBlank()) {
                try {
                    org.springframework.web.client.RestClient.create().post()
                        .uri(colabBaseUrl + "/api/internal/collaboration/push-notification")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .body(Map.of(
                            "token", u.getFcmToken(),
                            "title", "Nueva Notificación",
                            "body", dto.mensaje()
                        ))
                        .retrieve()
                        .toBodilessEntity();
                } catch (Exception e) {
                    System.err.println("No se pudo contactar al servidor de colaboración para Push manual: " + e.getMessage());
                }
            }
        });
        
        return response;
    }

    public List<NotificacionResponseDTO> obtenerTodas() {
        return notificacionRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<NotificacionResponseDTO> obtenerPorUsuario(String usuarioId) {
        return notificacionRepository.findByUsuarioId(usuarioId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public NotificacionResponseDTO marcarLeida(String id) {
        Notificacion n = notificacionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notificación no encontrada con ID: " + id));
        n.setLeido(true);
        return mapToDTO(notificacionRepository.save(n));
    }

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
