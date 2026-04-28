package com.sw.api.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sw.api.dtos.IaAnalisisResponseDTO;
import com.sw.api.dtos.TareaResponseDTO;
import com.sw.api.models.Tarea;
import com.sw.api.repositories.TareaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IaAnalisisService {

    private final TareaRepository tareaRepository;
    private final RestClient.Builder restClientBuilder;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    public IaAnalisisResponseDTO analizarTarea(String tareaId) {
        Tarea tarea = tareaRepository.findById(tareaId)
                .orElseThrow(() -> new RuntimeException("Tarea no encontrada con ID: " + tareaId));

        // Construir el prompt contextualizado con los datos reales de la tarea
        String prompt = buildPrompt(tarea);

        // Llamar a Gemini API
        String respuestaIA = llamarGemini(prompt);

        // Parsear y estructurar la respuesta
        return parsearRespuesta(tareaId, respuestaIA, tarea.getDatos());
    }

    public TareaResponseDTO priorizarTarea(String tareaId) {
        Tarea tarea = tareaRepository.findById(tareaId)
                .orElseThrow(() -> new RuntimeException("Tarea no encontrada con ID: " + tareaId));

        String prompt = buildPromptPrioridad(tarea);
        String respuestaIA = llamarGemini(prompt);

        // Extraer la prioridad de la respuesta: esperamos ALTA, MEDIA o BAJA
        String prioridad = extraerPrioridad(respuestaIA);
        tarea.setPrioridad(prioridad);

        Tarea guardada = tareaRepository.save(tarea);
        return new TareaResponseDTO(
                guardada.getId(),
                guardada.getWorkflowId(),
                guardada.getEstado(),
                guardada.getPasoActual(),
                guardada.getAsignadoA(),
                guardada.getPrioridad(),
                guardada.getDatos(),
                guardada.getDocumentosUrl(),
                guardada.getHistorial(),
                guardada.getComentarios(),
                null // Formulario no requerido para el análisis de prioridad
        );
    }

    private String buildPromptPrioridad(Tarea tarea) {
        StringBuilder sb = new StringBuilder();
        sb.append("Eres un clasificador experto de prioridades en procesos empresariales. ");
        sb.append("Analiza los datos de la siguiente tarea y determina su nivel de prioridad.\n\n");
        sb.append("=== DATOS DE LA TAREA ===\n");
        sb.append("Estado: ").append(tarea.getEstado()).append("\n");
        sb.append("Paso actual: ").append(tarea.getPasoActual()).append("\n");
        sb.append("Assigned to: ").append(tarea.getAsignadoA()).append("\n");
        sb.append("\n=== DATOS DEL FORMULARIO ===\n");
        if (tarea.getDatos() != null) {
            tarea.getDatos().forEach((k, v) -> sb.append("- ").append(k).append(": ").append(v).append("\n"));
        }
        sb.append("\n=== HISTORIAL ===\n");
        if (tarea.getHistorial() != null) {
            sb.append("Cantidad de eventos: ").append(tarea.getHistorial().size()).append("\n");
        }
        sb.append("\n=== INSTRUCCIÓN ===\n");
        sb.append("Clasifica la prioridad de esta tarea como exactamente una de estas opciones: ALTA, MEDIA o BAJA.\n");
        sb.append("Considera factores como: montos elevados, urgencia implícita en los datos, cantidad de pasos ya ejecutados.\n");
        sb.append("Responde ÚNICAMENTE con el formato exacto:\n");
        sb.append("PRIORIDAD: [ALTA|MEDIA|BAJA]\n");
        sb.append("JUSTIFICACION: [una sola frase explicando el motivo]\n");
        return sb.toString();
    }

    private String extraerPrioridad(String respuesta) {
        try {
            if (respuesta.contains("PRIORIDAD:")) {
                String linea = respuesta.lines()
                        .filter(l -> l.startsWith("PRIORIDAD:"))
                        .findFirst().orElse("");
                String valor = linea.replace("PRIORIDAD:", "").trim().toUpperCase();
                if (valor.contains("ALTA")) return "ALTA";
                if (valor.contains("BAJA")) return "BAJA";
                return "MEDIA"; // default seguro
            }
        } catch (Exception ignored) {}
        return "MEDIA";
    }

    private String buildPrompt(Tarea tarea) {
        StringBuilder sb = new StringBuilder();
        sb.append("Eres un asistente experto en análisis de procesos empresariales y workflows. ");
        sb.append("Analiza la siguiente tarea y proporciona recomendaciones claras para el funcionario responsable.\n\n");
        sb.append("=== DATOS DE LA TAREA ===\n");
        sb.append("ID: ").append(tarea.getId()).append("\n");
        sb.append("Workflow ID: ").append(tarea.getWorkflowId()).append("\n");
        sb.append("Estado actual: ").append(tarea.getEstado()).append("\n");
        sb.append("Paso actual: ").append(tarea.getPasoActual()).append("\n");
        sb.append("Asignado a: ").append(tarea.getAsignadoA()).append("\n");
        sb.append("\n=== DATOS DEL FORMULARIO ===\n");
        if (tarea.getDatos() != null) {
            tarea.getDatos().forEach((k, v) -> sb.append("- ").append(k).append(": ").append(v).append("\n"));
        }
        sb.append("\n=== HISTORIAL ===\n");
        if (tarea.getHistorial() != null) {
            tarea.getHistorial().forEach(h ->
                sb.append("- [").append(h.getFecha()).append("] ")
                  .append(h.getAccion()).append(": ").append(h.getDetalle()).append("\n")
            );
        }
        sb.append("\n=== INSTRUCCIONES ===\n");
        sb.append("1. Proporciona un análisis breve del estado de la tarea.\n");
        sb.append("2. Lista exactamente 3 recomendaciones concretas para el funcionario, separadas por |.\n");
        sb.append("3. Indica si hay algún riesgo o irregularidad detectada.\n");
        sb.append("\nFormato de respuesta esperado:\n");
        sb.append("ANALISIS: [tu análisis aquí]\n");
        sb.append("RECOMENDACIONES: [recomendación 1]|[recomendación 2]|[recomendación 3]\n");
        return sb.toString();
    }

    private String llamarGemini(String prompt) {
        RestClient client = restClientBuilder.build();

        // Construir el body del request según la API de Gemini
        Map<String, Object> requestBody = Map.of(
            "contents", List.of(
                Map.of("parts", List.of(
                    Map.of("text", prompt)
                ))
            )
        );

        String urlConKey = geminiApiUrl + "?key=" + geminiApiKey;

        try {
            String response = client.post()
                    .uri(urlConKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            // Extraer el texto de la respuesta JSON de Gemini
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);
            return root
                    .path("candidates").get(0)
                    .path("content")
                    .path("parts").get(0)
                    .path("text")
                    .asText("Sin respuesta de IA");
        } catch (Exception e) {
            return "ANALISIS: No se pudo completar el análisis automático.\nRECOMENDACIONES: Revisar manualmente|Contactar al administrador|Verificar datos del formulario";
        }
    }

    private IaAnalisisResponseDTO parsearRespuesta(String tareaId, String respuesta, Map<String, Object> datos) {
        String analisis = "Análisis no disponible";
        List<String> recomendaciones = List.of("Sin recomendaciones");

        try {
            if (respuesta.contains("ANALISIS:")) {
                String[] partes = respuesta.split("RECOMENDACIONES:");
                analisis = partes[0].replace("ANALISIS:", "").trim();
                if (partes.length > 1) {
                    recomendaciones = Arrays.stream(partes[1].trim().split("\\|"))
                            .map(String::trim)
                            .filter(s -> !s.isBlank())
                            .collect(Collectors.toList());
                }
            } else {
                analisis = respuesta;
            }
        } catch (Exception ignored) {
            analisis = respuesta;
        }

        return new IaAnalisisResponseDTO(tareaId, analisis, recomendaciones, datos);
    }
}
