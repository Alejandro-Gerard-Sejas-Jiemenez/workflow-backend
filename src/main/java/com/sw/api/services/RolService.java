package com.sw.api.services;

import com.sw.api.dtos.RolResponseDTO;
import com.sw.api.repositories.RolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RolService {

    private final RolRepository rolRepository;

    public List<RolResponseDTO> obtenerTodos() {
        return rolRepository.findAll().stream()
                .map(rol -> new RolResponseDTO(rol.getId(), rol.getNombre()))
                .toList();
    }
}
