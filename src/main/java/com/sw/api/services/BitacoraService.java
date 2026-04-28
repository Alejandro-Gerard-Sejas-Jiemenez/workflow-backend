package com.sw.api.services;

import com.sw.api.dtos.BitacoraDTO;
import com.sw.api.models.Bitacora;
import com.sw.api.models.Usuario;
import com.sw.api.repositories.BitacoraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BitacoraService {

    private final BitacoraRepository bitacoraRepository;

    public void registrarAccion(String accion, String entidad, String detalles) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        Object principal = (auth != null) ? auth.getPrincipal() : null;
        
        Bitacora bitacora = new Bitacora();
        bitacora.setAccion(accion);
        bitacora.setEntidad(entidad);
        bitacora.setDetalles(detalles);

        if (principal instanceof Usuario usuario) {
            bitacora.setUsuarioId(usuario.getId());
            bitacora.setUsuarioNombre(usuario.getNombre());
        } else {
            bitacora.setUsuarioId("SISTEMA");
            bitacora.setUsuarioNombre("Sistema");
        }

        bitacoraRepository.save(bitacora);
    }

    public List<BitacoraDTO> obtenerLogs() {
        return bitacoraRepository.findAllByOrderByFechaDesc().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private BitacoraDTO mapToDTO(Bitacora b) {
        return new BitacoraDTO(
                b.getId(),
                b.getUsuarioId(),
                b.getUsuarioNombre(),
                b.getAccion(),
                b.getEntidad(),
                b.getFecha(),
                b.getDetalles()
        );
    }
}
