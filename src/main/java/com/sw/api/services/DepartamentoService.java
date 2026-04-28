package com.sw.api.services;

import com.sw.api.dtos.DepartamentoDTO;
import com.sw.api.models.Departamento;
import com.sw.api.repositories.DepartamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DepartamentoService {

    private final DepartamentoRepository departamentoRepository;
    private final BitacoraService bitacoraService;

    public List<DepartamentoDTO> obtenerTodos() {
        return departamentoRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public DepartamentoDTO crear(DepartamentoDTO dto) {
        if (departamentoRepository.findByNombre(dto.nombre()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe un departamento con ese nombre");
        }
        
        Departamento departamento = new Departamento();
        departamento.setNombre(dto.nombre());
        departamento.setDescripcion(dto.descripcion());
        if (dto.estado() != null && !dto.estado().isBlank()) {
            departamento.setEstado(dto.estado());
        }

        Departamento saved = departamentoRepository.save(departamento);
        bitacoraService.registrarAccion("CREAR_DEPARTAMENTO", "Departamento", "Se creó el departamento " + saved.getNombre());
        return mapToDTO(saved);
    }

    public DepartamentoDTO actualizar(String id, DepartamentoDTO dto) {
        Departamento departamento = departamentoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Departamento no encontrado"));

        if (!departamento.getNombre().equals(dto.nombre()) && 
            departamentoRepository.findByNombre(dto.nombre()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe otro departamento con ese nombre");
        }

        departamento.setNombre(dto.nombre());
        departamento.setDescripcion(dto.descripcion());
        if (dto.estado() != null && !dto.estado().isBlank()) {
            departamento.setEstado(dto.estado());
        }

        Departamento saved = departamentoRepository.save(departamento);
        bitacoraService.registrarAccion("ACTUALIZAR_DEPARTAMENTO", "Departamento", "Se actualizó el departamento " + saved.getNombre());
        return mapToDTO(saved);
    }

    public void eliminar(String id) {
        Departamento departamento = departamentoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Departamento no encontrado"));
                
        // Eliminación lógica
        departamento.setEstado("INACTIVO");
        departamentoRepository.save(departamento);
        bitacoraService.registrarAccion("DESACTIVAR_DEPARTAMENTO", "Departamento", "Se desactivó el departamento " + departamento.getNombre());
    }

    private DepartamentoDTO mapToDTO(Departamento d) {
        return new DepartamentoDTO(d.getId(), d.getNombre(), d.getDescripcion(), d.getEstado());
    }
}
