package com.sw.api.ia.controllers;

import com.sw.api.ia.services.AIOrchestratorService;
import com.sw.api.ia.services.AIOrchestratorService.AiGenerationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/ai-orchestrator")
@RequiredArgsConstructor
public class AIOrchestratorController {

    private final AIOrchestratorService aiOrchestratorService;

    @PostMapping("/generate-from-prompt")
    @PreAuthorize("hasAuthority('ROLE_DESIGNER')")
    public ResponseEntity<Map<String, String>> generateFromPrompt(@RequestBody Map<String, String> request) {
        String prompt = request.get("prompt");
        if (prompt == null || prompt.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Debes describir el proceso antes de generar el workflow."));
        }

        AiGenerationResult result = aiOrchestratorService.generateFromPromptDetailed(prompt);
        return buildAiResponse(result, "La IA no pudo generar un BPMN valido desde el texto enviado.");
    }

    @PostMapping("/propose-workflow")
    @PreAuthorize("hasAuthority('ROLE_DESIGNER')")
    public ResponseEntity<Object> proposeWorkflow(@RequestBody Map<String, String> request) {
        String prompt = request.get("prompt");
        if (prompt == null || prompt.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Debes describir el proceso."));
        }
        return ResponseEntity.ok(aiOrchestratorService.proposeWorkflowJson(prompt));
    }

    @PostMapping("/generate-from-voice")
    @PreAuthorize("hasAuthority('ROLE_DESIGNER')")
    public ResponseEntity<Map<String, String>> generateFromVoice(@RequestParam("audio") MultipartFile audio) throws IOException {
        if (audio.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El archivo de audio esta vacio."));
        }

        AiGenerationResult result = aiOrchestratorService.generateFromVoiceDetailed(audio.getBytes(), audio.getContentType());
        return buildAiResponse(result, "La IA no pudo generar un BPMN valido desde el audio enviado.");
    }

    @PostMapping("/generate-from-pdf")
    @PreAuthorize("hasAuthority('ROLE_DESIGNER')")
    public ResponseEntity<Map<String, String>> generateFromPDF(@RequestParam("pdf") MultipartFile pdf) throws IOException {
        if (pdf.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El archivo PDF esta vacio."));
        }

        AiGenerationResult result = aiOrchestratorService.generateFromPDFDetailed(pdf.getBytes());
        return buildAiResponse(result, "La IA no pudo generar un BPMN valido desde el PDF enviado.");
    }

    private ResponseEntity<Map<String, String>> buildAiResponse(AiGenerationResult result, String emptyXmlMessage) {
        if (result == null || result.xml() == null || result.xml().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                    "error", result != null && result.error() != null && !result.error().isBlank() ? result.error() : emptyXmlMessage,
                    "extractedTextPreview", safeValue(result != null ? result.extractedTextPreview() : null),
                    "rawResponsePreview", safeValue(result != null ? result.rawResponsePreview() : null),
                    "cleanedXmlPreview", safeValue(result != null ? result.cleanedXmlPreview() : null),
                    "repairedXmlPreview", safeValue(result != null ? result.repairedXmlPreview() : null)
            ));
        }

        return ResponseEntity.ok(Map.of("xml", result.xml()));
    }

    private String safeValue(String value) {
        return value == null ? "" : value;
    }
}
