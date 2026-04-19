package com.sw.api.services;

import com.sw.api.models.Formulario;
import com.sw.api.dtos.FormularioCreateDTO;
import com.sw.api.dtos.FormularioResponseDTO;
import com.sw.api.dtos.FormularioCamposUpdateDTO;
import com.sw.api.repositories.FormularioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FormularioService {

    private final FormularioRepository formularioRepository;

    public FormularioResponseDTO crear(FormularioCreateDTO dto) {
        Formulario form = new Formulario();
        form.setNombre(dto.nombre());
        
        List<Formulario.Campo> campos = dto.campos().stream().map(c ->
            new Formulario.Campo(c.nombre(), c.tipo(), c.isRequerido(), c.opciones())
        ).collect(Collectors.toList());
        form.setCampos(campos);

        if (dto.reglasVisibilidad() != null) {
            form.setReglasVisibilidad(dto.reglasVisibilidad());
        }

        return mapToDTO(formularioRepository.save(form));
    }

    public FormularioResponseDTO actualizarCampos(String id, FormularioCamposUpdateDTO dto) {
        Formulario form = formularioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Formulario no encontrado con ID: " + id));

        List<Formulario.Campo> campos = dto.campos().stream().map(c ->
            new Formulario.Campo(c.nombre(), c.tipo(), c.isRequerido(), c.opciones())
        ).collect(Collectors.toList());
        
        form.setCampos(campos);
        return mapToDTO(formularioRepository.save(form));
    }

    public List<FormularioResponseDTO> obtenerTodos() {
        return formularioRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public FormularioResponseDTO obtenerPorId(String id) {
        Formulario form = formularioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Formulario no encontrado con ID: " + id));
        return mapToDTO(form);
    }

    private FormularioResponseDTO mapToDTO(Formulario f) {
        return new FormularioResponseDTO(
            f.getId(),
            f.getNombre(),
            f.getCampos(),
            f.getReglasVisibilidad()
        );
    }
}
