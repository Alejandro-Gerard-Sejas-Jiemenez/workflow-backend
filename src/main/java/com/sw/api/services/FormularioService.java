package com.sw.api.services;

import com.sw.api.models.Formulario;
import com.sw.api.repositories.FormularioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FormularioService {

    private final FormularioRepository formularioRepository;

    public List<Formulario> obtenerTodos() {
        return formularioRepository.findAll();
    }

    public Formulario obtenerPorId(String id) {
        return formularioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Formulario no encontrado con ID: " + id));
    }
}
