package com.sw.api.services;

import com.sw.api.models.Tarea;
import com.sw.api.models.Usuario;
import com.sw.api.models.Workflow;
import com.sw.api.dtos.TareaCreateDTO;
import com.sw.api.dtos.TareaResponseDTO;
import com.sw.api.dtos.WorkflowResponseDTO;
import com.sw.api.repositories.TareaRepository;
import com.sw.api.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TareaService {

    private final TareaRepository tareaRepository;
    private final WorkflowService workflowService;
    private final UsuarioRepository usuarioRepository;

    public TareaResponseDTO iniciarTarea(TareaCreateDTO dto, String username) {
        // Encontrar al actor que inicia por su email asociado en el Token
        Usuario usuario = usuarioRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Extraer el catálogo para buscar la delegación del Paso 1
        WorkflowResponseDTO workflow = workflowService.obtenerPorId(dto.workflowId());
        
        Workflow.Paso pasoInicial = workflow.pasos().stream()
                .filter(p -> p.getOrden() == 1)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("El Workflow no tiene un Paso 1 configurado."));

        // Construir instancia nativa
        Tarea tarea = new Tarea();
        tarea.setWorkflowId(workflow.id());
        tarea.setEstado("EN_PROGRESO");
        tarea.setPasoActual(1);
        tarea.setAsignadoA(pasoInicial.getDepartamento()); // Asignación dinámica inferida del Catálogo
        tarea.setDatos(dto.datos());

        // Inyectar Historial auto-generado
        Tarea.Historial primerHistorial = new Tarea.Historial(
                usuario.getId(),
                "INICIO_TAREA",
                "Tarea iniciada desde el formulario del cliente",
                LocalDateTime.now()
        );
        
        List<Tarea.Historial> historialList = new ArrayList<>();
        historialList.add(primerHistorial);
        tarea.setHistorial(historialList);

        return mapToDTO(tareaRepository.save(tarea));
    }

    private TareaResponseDTO mapToDTO(Tarea t) {
        return new TareaResponseDTO(
                t.getId(),
                t.getWorkflowId(),
                t.getEstado(),
                t.getPasoActual(),
                t.getAsignadoA(),
                t.getDatos(),
                t.getHistorial()
        );
    }
}
