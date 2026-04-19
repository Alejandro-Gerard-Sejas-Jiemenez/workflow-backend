package com.sw.api.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sw.api.dtos.IaAnalisisResponseDTO;
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
