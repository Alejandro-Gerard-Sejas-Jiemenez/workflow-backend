package com.sw.api.ia.services;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIOrchestratorService {

    private static final int MAX_PDF_TEXT_CHARS = 12000;
    private static final int LOG_PREVIEW_CHARS = 1200;

    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${groq.api.key}")
    private String groqApiKey;

    @Value("${groq.api.url}")
    private String groqApiUrl;

    @Value("${groq.model}")
    private String groqModel;

    @Value("${workflow.ia.enabled:true}")
    private boolean workflowIaEnabled;

    @Value("${workflow.ia.base-url:http://localhost:8001}")
    private String workflowIaBaseUrl;

    @Value("${workflow.ia.confidence-threshold:0.7}")
    private double workflowIaConfidenceThreshold;

    // Layout constants
    private static final int POOL_X = 160;
    private static final int POOL_Y = 80;
    private static final int POOL_LABEL_W = 30;
    private static final int LANE_HEIGHT = 160;
    private static final int FIRST_NODE_X = 270;
    private static final int NODE_X_STEP = 180;
    private static final int TASK_W = 120;
    private static final int TASK_H = 80;
    private static final int GW_SIZE = 50;
    private static final int EVT_SIZE = 36;

    // =========================================================================
    // Generacion desde Prompt (Texto)
    // =========================================================================
    public String generateFromPrompt(String prompt) {
        return generateFromPromptDetailed(prompt).xml();
    }

    public AiGenerationResult generateFromPromptDetailed(String prompt) {
        try {
            return generateHybridBpmn(prompt, null);
        } catch (Exception e) {
            log.error("Error generating from prompt", e);
            return failureResult("Error interno al generar BPMN desde texto: " + safeMessage(e), null, null, null, null);
        }
    }

    public Object proposeWorkflowJson(String prompt) {
        return analyzeWithWorkflowIa(prompt);
    }

    // =========================================================================
    // Generacion desde Voz
    // =========================================================================
    public String generateFromVoice(byte[] audioData, String mimeType) {
        return generateFromVoiceDetailed(audioData, mimeType).xml();
    }

    public AiGenerationResult generateFromVoiceDetailed(byte[] audioData, String mimeType) {
        try {
            String transcript = transcribeWithWhisper(audioData, mimeType);
            if (transcript == null || transcript.isBlank()) {
                return failureResult("La transcripcion del audio llego vacia.", null, null, null, null);
            }
            log.info("Transcription: {}", transcript);
            return generateHybridBpmn(transcript, toLogPreview(transcript));
        } catch (Exception e) {
            log.error("Error generating from voice", e);
            return failureResult("Error interno al generar BPMN desde audio: " + safeMessage(e), null, null, null, null);
        }
    }

    // =========================================================================
    // Generacion desde PDF
    // =========================================================================
    public String generateFromPDF(byte[] pdfData) {
        return generateFromPDFDetailed(pdfData).xml();
    }

    public AiGenerationResult generateFromPDFDetailed(byte[] pdfData) {
        try {
            String extractedText = extractTextFromPdf(pdfData);
            return generateHybridBpmn(extractedText, toLogPreview(extractedText));
        } catch (Exception e) {
            log.error("Error generating from PDF", e);
            return failureResult("Error interno al generar BPMN desde PDF: " + safeMessage(e), null, null, null, null);
        }
    }

    // =========================================================================
    // Llamada a Groq
    // =========================================================================
    private String callGroq(String systemPrompt, String userPrompt) throws Exception {
        RestClient client = restClientBuilder.build();

        Map<String, Object> body = Map.of(
                "model", groqModel,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0.1,
                "max_tokens", 4096
        );

        String res = client.post()
                .uri(groqApiUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + groqApiKey)
                .body(body)
                .retrieve()
                .body(String.class);

        log.debug("Groq raw response: {}", res);
        JsonNode root = objectMapper.readTree(res);
        JsonNode choices = root.path("choices");

        if (choices.isMissingNode() || choices.isEmpty()) {
            log.error("Groq returned no choices. Full response: {}", res);
            return "";
        }

        return choices.get(0).path("message").path("content").asText("").trim();
    }

    private String transcribeWithWhisper(byte[] audioData, String mimeType) throws Exception {
        RestClient client = restClientBuilder.build();
        String boundary = "----FormBoundary" + System.currentTimeMillis();
        String extension = (mimeType != null && mimeType.contains("webm")) ? "webm"
                : (mimeType != null && mimeType.contains("ogg")) ? "ogg" : "wav";

        byte[] header = ("--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"file\"; filename=\"audio." + extension + "\"\r\n" +
                "Content-Type: " + (mimeType != null ? mimeType : "audio/wav") + "\r\n\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] middle = ("\r\n--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"model\"\r\n\r\nwhisper-large-v3\r\n--" + boundary + "--\r\n")
                .getBytes(StandardCharsets.UTF_8);

        byte[] body = new byte[header.length + audioData.length + middle.length];
        System.arraycopy(header, 0, body, 0, header.length);
        System.arraycopy(audioData, 0, body, header.length, audioData.length);
        System.arraycopy(middle, 0, body, header.length + audioData.length, middle.length);

        String res = client.post()
                .uri("https://api.groq.com/openai/v1/audio/transcriptions")
                .contentType(MediaType.parseMediaType("multipart/form-data; boundary=" + boundary))
                .header("Authorization", "Bearer " + groqApiKey)
                .body(body)
                .retrieve()
                .body(String.class);

        return objectMapper.readTree(res).path("text").asText("").trim();
    }

    private AiGenerationResult generateHybridBpmn(String sourceText, String extractedTextPreview) throws Exception {
        LocalAnalysisResponse localAnalysis = analyzeWithWorkflowIa(sourceText);
        if (canGenerateLocally(localAnalysis)) {
            String localXml = buildSemanticBpmnFromLocalAnalysis(localAnalysis);
            String localXmlPreview = toLogPreview(localXml);

            if (isValidBpmnXml(localXml)) {
                log.info("BPMN generated locally through workflow-ia with confidence={}", localAnalysis.resolvedConfidence());
                return new AiGenerationResult(
                        injectDiagramBlock(localXml),
                        null,
                        extractedTextPreview,
                        toLogPreview(localAnalysis.toSummary()),
                        localXmlPreview,
                        null
                );
            }

            log.warn("Local BPMN generation produced invalid XML. Falling back to Groq.");
        }

        String promptForGroq = buildGroqPromptFromSource(sourceText);
        return generateValidatedBpmn(promptForGroq, extractedTextPreview);
    }

    private LocalAnalysisResponse analyzeWithWorkflowIa(String sourceText) {
        if (!workflowIaEnabled || sourceText == null || sourceText.isBlank()) {
            return null;
        }

        try {
            RestClient client = restClientBuilder.build();
            return client.post()
                    .uri(workflowIaBaseUrl + "/analyze")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("text", sourceText))
                    .retrieve()
                    .body(LocalAnalysisResponse.class);
        } catch (RestClientException e) {
            log.warn("workflow-ia is not available. Falling back to Groq. Cause: {}", e.getMessage());
            return null;
        }
    }

    private boolean canGenerateLocally(LocalAnalysisResponse localAnalysis) {
        return localAnalysis != null
                && !localAnalysis.requiresFallback()
                && localAnalysis.resolvedConfidence() >= workflowIaConfidenceThreshold
                && localAnalysis.activities() != null
                && !localAnalysis.activities().isEmpty();
    }

    private String buildGroqPromptFromSource(String sourceText) {
        if (sourceText == null || sourceText.isBlank()) {
            return "Genera un XML BPMN 2.0 para un proceso de negocio generico.";
        }

        return """
            Analiza el siguiente texto y genera XML BPMN 2.0 fiel al proceso descrito.
            Requisitos:
            - Detecta el nombre del proceso si aparece.
            - Identifica departamentos o responsables y crea carriles BPMN.
            - Coloca cada actividad en el carril correcto mediante flowNodeRef.
            - Conserva decisiones, aprobaciones, rechazos, validaciones y pasos finales si aparecen.
            - Si el texto es ambiguo, completa solo lo minimo necesario sin inventar departamentos irrelevantes.
            - Mantiene el flujo principal de inicio a fin.

            Texto:
            """ + sourceText;
    }

    private AiGenerationResult generateValidatedBpmn(String userPrompt, String extractedTextPreview) throws Exception {
        String raw = callGroq(getSystemPrompt(), userPrompt);
        String rawPreview = toLogPreview(raw);
        log.warn("AI raw BPMN response preview: {}", rawPreview);
        String cleaned = cleanXmlResponse(raw);
        String cleanedPreview = toLogPreview(cleaned);
        log.warn("AI cleaned BPMN response preview: {}", cleanedPreview);
        String repairedPreview = null;

        if (!isValidBpmnXml(cleaned)) {
            log.warn("AI returned invalid BPMN XML on first attempt. Trying repair.");
            cleaned = repairSemanticXml(cleaned);
            repairedPreview = toLogPreview(cleaned);
            log.warn("AI repaired BPMN response preview: {}", repairedPreview);
        }

        if (!isValidBpmnXml(cleaned)) {
            log.error("AI returned unrecoverable BPMN XML. Raw response: {}", raw);
            return failureResult(
                    "La IA devolvio XML que no pudo validarse como BPMN 2.0.",
                    extractedTextPreview,
                    rawPreview,
                    cleanedPreview,
                    repairedPreview
            );
        }

        String xmlWithLayout = injectDiagramBlock(cleaned);
        return new AiGenerationResult(xmlWithLayout, null, extractedTextPreview, rawPreview, cleanedPreview, repairedPreview);
    }

    private String extractTextFromPdf(byte[] pdfData) throws Exception {
        if (pdfData == null || pdfData.length == 0) {
            return "";
        }

        try (PDDocument document = Loader.loadPDF(pdfData)) {
            PDFTextStripper textStripper = new PDFTextStripper();
            textStripper.setSortByPosition(true);

            String extracted = textStripper.getText(document);
            String normalized = normalizeExtractedText(extracted);
            log.info("PDF text extracted: {} chars, {} pages", normalized.length(), document.getNumberOfPages());
            log.warn("PDF text preview: {}", toLogPreview(normalized));
            return normalized;
        }
    }

    private String normalizeExtractedText(String text) {
        if (text == null) {
            return "";
        }

        String normalized = text
                .replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();

        if (normalized.length() > MAX_PDF_TEXT_CHARS) {
            normalized = normalized.substring(0, MAX_PDF_TEXT_CHARS);
        }

        return normalized;
    }

    private String buildRepairPrompt(String invalidXml) {
        return """
            Corrige el siguiente XML BPMN 2.0 para que sea XML valido y semanticamente consistente.
            Reglas obligatorias:
            - Devuelve solo XML puro.
            - Conserva el proceso original lo maximo posible.
            - Debe existir bpmn:definitions.
            - Debe existir un bpmn:process.
            - Incluye incoming/outgoing coherentes en cada nodo.
            - Incluye sequenceFlow con sourceRef y targetRef validos.
            - Usa laneSet y lane cuando existan departamentos o responsables.
            - No incluyas markdown, comentarios ni explicaciones.

            XML a corregir:
            """ + invalidXml;
    }

    private String getSystemPrompt() {
        return """
            Eres un Arquitecto BPMN 2.0 experto. Genera EXCLUSIVAMENTE XML puro, sin markdown, sin backticks.

            ESTRUCTURA OBLIGATORIA:
            <bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                              xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                              xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
                              xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
                              id="Definitions_1" targetNamespace="http://bpmn.io/schema/bpmn">
              <bpmn:collaboration id="Collaboration_1">
                <bpmn:participant id="Participant_1" name="NOMBRE_PROCESO" processRef="Process_1"/>
              </bpmn:collaboration>
              <bpmn:process id="Process_1" isExecutable="true">
                <bpmn:laneSet id="LaneSet_1">
                  <bpmn:lane id="Lane_1" name="TI">
                    <bpmn:flowNodeRef>Task_1</bpmn:flowNodeRef>
                  </bpmn:lane>
                </bpmn:laneSet>
                <bpmn:startEvent id="StartEvent_1" name="Inicio">
                  <bpmn:outgoing>Flow_1</bpmn:outgoing>
                </bpmn:startEvent>
                ...tareas, gateways, flows...
                <bpmn:endEvent id="EndEvent_1" name="Fin">
                  <bpmn:incoming>Flow_N</bpmn:incoming>
                </bpmn:endEvent>
                <bpmn:sequenceFlow id="Flow_1" sourceRef="StartEvent_1" targetRef="Task_1"/>
              </bpmn:process>
            </bpmn:definitions>

            REGLAS:
            1.a Usa <bpmn:laneSet> y <bpmn:lane> para agrupar actividades por departamento o responsable cuando sea posible.
            1.b Los carriles deben representar departamentos del negocio, por ejemplo: TI, Recursos Humanos, Finanzas, Operaciones, Atencion al Cliente, Gerencia.
            1.c Cada actividad debe pertenecer a un carril mediante <bpmn:flowNodeRef>.
            1. <bpmn:process> es HERMANO de <bpmn:collaboration>, NUNCA anidado dentro.
            2. userTask=humanos, serviceTask=sistemas, exclusiveGateway=decisiones con name="Si"/"No".
            3. Incluye <bpmn:incoming> y <bpmn:outgoing> en CADA nodo.
            4. NO incluyas coordenadas ni bpmndi. Solo XML semantico puro.
            """;
    }

    private String buildSemanticBpmnFromLocalAnalysis(LocalAnalysisResponse analysis) {
        List<LocalActivity> activities = analysis.activities() == null ? List.of() : analysis.activities();
        List<String> departments = resolveLaneOrder(analysis, activities);
        Map<String, String> activityLaneById = new LinkedHashMap<>();
        Map<String, List<String>> laneFlowNodes = new LinkedHashMap<>();

        for (String department : departments) {
            laneFlowNodes.put(department, new ArrayList<>());
        }

        for (LocalActivity activity : activities) {
            String lane = resolveLaneForActivity(activity, departments);
            activityLaneById.put(activity.id(), lane);
            laneFlowNodes.computeIfAbsent(lane, key -> new ArrayList<>()).add(activity.id());
        }

        boolean includeDecision = analysis.decisions() != null && !analysis.decisions().isEmpty() && activities.size() >= 2;
        String decisionId = includeDecision ? safeId(analysis.decisions().getFirst().id(), "Gateway_1") : null;
        String processName = escapeXml(analysis.processName() == null || analysis.processName().isBlank() ? "Proceso automatico" : analysis.processName());

        StringBuilder xml = new StringBuilder();
        xml.append("""
                <?xml version="1.0" encoding="UTF-8"?>
                <bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                                  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                                  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
                                  xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
                                  id="Definitions_1" targetNamespace="http://bpmn.io/schema/bpmn">
                """);
        xml.append("  <bpmn:collaboration id=\"Collaboration_1\">\n");
        xml.append("    <bpmn:participant id=\"Participant_1\" name=\"").append(processName).append("\" processRef=\"Process_1\"/>\n");
        xml.append("  </bpmn:collaboration>\n");
        xml.append("  <bpmn:process id=\"Process_1\" isExecutable=\"true\">\n");

        if (!departments.isEmpty()) {
            xml.append("    <bpmn:laneSet id=\"LaneSet_1\">\n");
            for (int i = 0; i < departments.size(); i++) {
                String laneName = departments.get(i);
                xml.append("      <bpmn:lane id=\"Lane_").append(i + 1).append("\" name=\"").append(escapeXml(laneName)).append("\">\n");
                for (String flowNodeRef : laneFlowNodes.getOrDefault(laneName, List.of())) {
                    xml.append("        <bpmn:flowNodeRef>").append(flowNodeRef).append("</bpmn:flowNodeRef>\n");
                }
                xml.append("      </bpmn:lane>\n");
            }
            xml.append("    </bpmn:laneSet>\n");
        }

        if (!includeDecision) {
            appendLinearProcessElements(xml, activities);
        } else {
            appendDecisionProcessElements(xml, activities, analysis.decisions().getFirst(), decisionId);
        }

        xml.append("  </bpmn:process>\n");
        xml.append("</bpmn:definitions>");
        return xml.toString();
    }

    private List<String> resolveLaneOrder(LocalAnalysisResponse analysis, List<LocalActivity> activities) {
        List<String> resolved = new ArrayList<>();

        if (analysis.departments() != null) {
            for (String department : analysis.departments()) {
                if (department != null && !department.isBlank() && !resolved.contains(department)) {
                    resolved.add(department);
                }
            }
        }

        for (LocalActivity activity : activities) {
            if (activity.actor() != null && !activity.actor().isBlank() && !resolved.contains(activity.actor())) {
                resolved.add(activity.actor());
            }
        }

        if (resolved.isEmpty()) {
            resolved.add("Operaciones");
        }

        return resolved;
    }

    private String resolveLaneForActivity(LocalActivity activity, List<String> departments) {
        if (activity.actor() != null && !activity.actor().isBlank()) {
            return activity.actor();
        }
        return departments.getFirst();
    }

    private void appendLinearProcessElements(StringBuilder xml, List<LocalActivity> activities) {
        xml.append("    <bpmn:startEvent id=\"StartEvent_1\" name=\"Inicio\">\n");
        xml.append("      <bpmn:outgoing>Flow_1</bpmn:outgoing>\n");
        xml.append("    </bpmn:startEvent>\n");

        if (activities.isEmpty()) {
            xml.append("    <bpmn:task id=\"Task_1\" name=\"Procesar solicitud\">\n");
            xml.append("      <bpmn:incoming>Flow_1</bpmn:incoming>\n");
            xml.append("      <bpmn:outgoing>Flow_2</bpmn:outgoing>\n");
            xml.append("    </bpmn:task>\n");
            xml.append("    <bpmn:endEvent id=\"EndEvent_1\" name=\"Fin\">\n");
            xml.append("      <bpmn:incoming>Flow_2</bpmn:incoming>\n");
            xml.append("    </bpmn:endEvent>\n");
            xml.append("    <bpmn:sequenceFlow id=\"Flow_1\" sourceRef=\"StartEvent_1\" targetRef=\"Task_1\"/>\n");
            xml.append("    <bpmn:sequenceFlow id=\"Flow_2\" sourceRef=\"Task_1\" targetRef=\"EndEvent_1\"/>\n");
            return;
        }

        for (int i = 0; i < activities.size(); i++) {
            LocalActivity activity = activities.get(i);
            String incomingId = "Flow_" + (i + 1);
            String outgoingId = "Flow_" + (i + 2);
            String taskTag = "serviceTask".equals(activity.taskType()) ? "bpmn:serviceTask" : "bpmn:userTask";

            xml.append("    <").append(taskTag).append(" id=\"").append(activity.id()).append("\" name=\"")
                    .append(escapeXml(activity.label())).append("\">\n");
            xml.append("      <bpmn:incoming>").append(incomingId).append("</bpmn:incoming>\n");
            if (i < activities.size() - 1) {
                xml.append("      <bpmn:outgoing>").append(outgoingId).append("</bpmn:outgoing>\n");
            } else {
                xml.append("      <bpmn:outgoing>").append(outgoingId).append("</bpmn:outgoing>\n");
            }
            xml.append("    </").append(taskTag).append(">\n");
        }

        xml.append("    <bpmn:endEvent id=\"EndEvent_1\" name=\"Fin\">\n");
        xml.append("      <bpmn:incoming>Flow_").append(activities.size() + 1).append("</bpmn:incoming>\n");
        xml.append("    </bpmn:endEvent>\n");
        xml.append("    <bpmn:sequenceFlow id=\"Flow_1\" sourceRef=\"StartEvent_1\" targetRef=\"").append(activities.getFirst().id()).append("\"/>\n");
        for (int i = 0; i < activities.size() - 1; i++) {
            xml.append("    <bpmn:sequenceFlow id=\"Flow_").append(i + 2).append("\" sourceRef=\"")
                    .append(activities.get(i).id()).append("\" targetRef=\"").append(activities.get(i + 1).id()).append("\"/>\n");
        }
        xml.append("    <bpmn:sequenceFlow id=\"Flow_").append(activities.size() + 1).append("\" sourceRef=\"")
                .append(activities.getLast().id()).append("\" targetRef=\"EndEvent_1\"/>\n");
    }

    private void appendDecisionProcessElements(StringBuilder xml, List<LocalActivity> activities, LocalDecision decision, String decisionId) {
        List<LocalActivity> preDecisionActivities = activities.size() > 2 ? activities.subList(0, activities.size() - 2) : List.of(activities.getFirst());
        LocalActivity yesActivity = activities.size() > 1 ? activities.get(activities.size() - 2) : activities.getFirst();
        LocalActivity noActivity = activities.getLast();
        String yesFlowId = "Flow_yes";
        String noFlowId = "Flow_no";
        String gatewayIncomingId = "Flow_gateway_in";

        xml.append("    <bpmn:startEvent id=\"StartEvent_1\" name=\"Inicio\">\n");
        xml.append("      <bpmn:outgoing>Flow_1</bpmn:outgoing>\n");
        xml.append("    </bpmn:startEvent>\n");

        String previousRef = "StartEvent_1";
        String previousFlowId = "Flow_1";
        int sequenceCounter = 2;

        for (LocalActivity activity : preDecisionActivities) {
            String taskTag = "serviceTask".equals(activity.taskType()) ? "bpmn:serviceTask" : "bpmn:userTask";
            String outgoingId = activity.id().equals(preDecisionActivities.getLast().id()) ? gatewayIncomingId : "Flow_" + sequenceCounter++;

            xml.append("    <").append(taskTag).append(" id=\"").append(activity.id()).append("\" name=\"")
                    .append(escapeXml(activity.label())).append("\">\n");
            xml.append("      <bpmn:incoming>").append(previousFlowId).append("</bpmn:incoming>\n");
            xml.append("      <bpmn:outgoing>").append(outgoingId).append("</bpmn:outgoing>\n");
            xml.append("    </").append(taskTag).append(">\n");

            xml.append("    <bpmn:sequenceFlow id=\"").append(previousFlowId).append("\" sourceRef=\"").append(previousRef)
                    .append("\" targetRef=\"").append(activity.id()).append("\"/>\n");

            previousRef = activity.id();
            previousFlowId = outgoingId;
        }

        xml.append("    <bpmn:exclusiveGateway id=\"").append(decisionId).append("\" name=\"")
                .append(escapeXml(decision.label())).append("\">\n");
        xml.append("      <bpmn:incoming>").append(previousFlowId).append("</bpmn:incoming>\n");
        xml.append("      <bpmn:outgoing>").append(yesFlowId).append("</bpmn:outgoing>\n");
        xml.append("      <bpmn:outgoing>").append(noFlowId).append("</bpmn:outgoing>\n");
        xml.append("    </bpmn:exclusiveGateway>\n");
        xml.append("    <bpmn:sequenceFlow id=\"").append(previousFlowId).append("\" sourceRef=\"").append(previousRef)
                .append("\" targetRef=\"").append(decisionId).append("\"/>\n");

        appendBranchTask(xml, yesActivity, yesFlowId, "Flow_end_yes");
        appendBranchTask(xml, noActivity, noFlowId, "Flow_end_no");

        xml.append("    <bpmn:endEvent id=\"EndEvent_1\" name=\"Fin\">\n");
        xml.append("      <bpmn:incoming>Flow_end_yes</bpmn:incoming>\n");
        xml.append("      <bpmn:incoming>Flow_end_no</bpmn:incoming>\n");
        xml.append("    </bpmn:endEvent>\n");
        xml.append("    <bpmn:sequenceFlow id=\"").append(yesFlowId).append("\" name=\"Si\" sourceRef=\"").append(decisionId)
                .append("\" targetRef=\"").append(yesActivity.id()).append("\"/>\n");
        xml.append("    <bpmn:sequenceFlow id=\"").append(noFlowId).append("\" name=\"No\" sourceRef=\"").append(decisionId)
                .append("\" targetRef=\"").append(noActivity.id()).append("\"/>\n");
        xml.append("    <bpmn:sequenceFlow id=\"Flow_end_yes\" sourceRef=\"").append(yesActivity.id()).append("\" targetRef=\"EndEvent_1\"/>\n");
        xml.append("    <bpmn:sequenceFlow id=\"Flow_end_no\" sourceRef=\"").append(noActivity.id()).append("\" targetRef=\"EndEvent_1\"/>\n");
    }

    private void appendBranchTask(StringBuilder xml, LocalActivity activity, String incomingId, String outgoingId) {
        String taskTag = "serviceTask".equals(activity.taskType()) ? "bpmn:serviceTask" : "bpmn:userTask";
        xml.append("    <").append(taskTag).append(" id=\"").append(activity.id()).append("\" name=\"")
                .append(escapeXml(activity.label())).append("\">\n");
        xml.append("      <bpmn:incoming>").append(incomingId).append("</bpmn:incoming>\n");
        xml.append("      <bpmn:outgoing>").append(outgoingId).append("</bpmn:outgoing>\n");
        xml.append("    </").append(taskTag).append(">\n");
    }

    private String safeId(String rawId, String fallbackId) {
        if (rawId == null || rawId.isBlank()) {
            return fallbackId;
        }
        return rawId.replaceAll("[^A-Za-z0-9_\\-]", "_");
    }

    private String escapeXml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    // =========================================================================
    // Inyeccion del bloque de Layout (sin re-serializar el DOM)
    // =========================================================================
    public String injectDiagramBlock(String semanticXml) {
        try {
            // Parsear solo para leer estructura, no para re-serializar.
            Document doc = parseXml(semanticXml);

            NodeList processes = doc.getElementsByTagNameNS("*", "process");
            if (processes.getLength() == 0) {
                log.error("No process element found in XML");
                return semanticXml;
            }

            Element processElement = (Element) processes.item(0);
            String processId = processElement.getAttribute("id");
            if (processId.isBlank()) {
                processId = "Process_1";
            }

            NodeList collabs = doc.getElementsByTagNameNS("*", "collaboration");
            String collabId = "Collaboration_1";
            String participantId = "Participant_1";
            if (collabs.getLength() > 0) {
                collabId = ((Element) collabs.item(0)).getAttribute("id");
                if (collabId.isBlank()) {
                    collabId = "Collaboration_1";
                }
                NodeList parts = ((Element) collabs.item(0)).getElementsByTagNameNS("*", "participant");
                if (parts.getLength() > 0) {
                    String pid = ((Element) parts.item(0)).getAttribute("id");
                    if (!pid.isBlank()) {
                        participantId = pid;
                    }
                }
            }

            List<String> laneOrder = new ArrayList<>();
            Map<String, String> nodeToLane = new HashMap<>();
            NodeList lanes = doc.getElementsByTagNameNS("*", "lane");
            for (int i = 0; i < lanes.getLength(); i++) {
                Element lane = (Element) lanes.item(i);
                String laneId = lane.getAttribute("id");
                laneOrder.add(laneId);
                NodeList fnRefs = lane.getElementsByTagNameNS("*", "flowNodeRef");
                for (int j = 0; j < fnRefs.getLength(); j++) {
                    nodeToLane.put(fnRefs.item(j).getTextContent().trim(), laneId);
                }
            }

            Map<String, String> nodeType = new HashMap<>();
            String[] tags = {"startEvent", "endEvent", "userTask", "serviceTask", "exclusiveGateway", "parallelGateway", "task"};
            for (String tag : tags) {
                NodeList nl = doc.getElementsByTagNameNS("*", tag);
                for (int i = 0; i < nl.getLength(); i++) {
                    String id = ((Element) nl.item(i)).getAttribute("id");
                    if (!id.isBlank()) {
                        nodeType.put(id, tag);
                    }
                }
            }

            List<FlowInfo> seqFlows = new ArrayList<>();
            Map<String, List<String>> adj = new HashMap<>();
            NodeList flows = doc.getElementsByTagNameNS("*", "sequenceFlow");
            for (int i = 0; i < flows.getLength(); i++) {
                Element f = (Element) flows.item(i);
                String src = f.getAttribute("sourceRef");
                String tgt = f.getAttribute("targetRef");
                String fid = f.getAttribute("id");
                adj.computeIfAbsent(src, key -> new ArrayList<>()).add(tgt);
                seqFlows.add(new FlowInfo(fid, src, tgt));
            }

            Map<String, Integer> depths = new HashMap<>();
            Queue<String> queue = new LinkedList<>();
            NodeList starts = doc.getElementsByTagNameNS("*", "startEvent");
            for (int i = 0; i < starts.getLength(); i++) {
                String id = ((Element) starts.item(i)).getAttribute("id");
                if (!id.isBlank()) {
                    depths.put(id, 0);
                    queue.add(id);
                }
            }

            nodeType.keySet().forEach(nodeId -> {
                if (!depths.containsKey(nodeId)) {
                    depths.put(nodeId, 0);
                    queue.add(nodeId);
                }
            });

            int maxDepth = 0;
            while (!queue.isEmpty()) {
                String current = queue.poll();
                int depth = depths.get(current);
                maxDepth = Math.max(maxDepth, depth);
                for (String neighbor : adj.getOrDefault(current, Collections.emptyList())) {
                    if (!depths.containsKey(neighbor)) {
                        depths.put(neighbor, depth + 1);
                        queue.add(neighbor);
                    }
                }
            }

            boolean hasLanes = !laneOrder.isEmpty();
            int effectiveLaneCount = hasLanes ? laneOrder.size() : 1;
            int poolWidth = FIRST_NODE_X + (maxDepth + 1) * NODE_X_STEP + 100;
            int poolHeight = effectiveLaneCount * LANE_HEIGHT;

            Map<String, int[]> coords = new LinkedHashMap<>();
            depths.forEach((id, depth) -> {
                int laneIdx = hasLanes ? Math.max(0, laneOrder.indexOf(nodeToLane.get(id))) : 0;
                String type = nodeType.getOrDefault(id, "task");
                int w = type.endsWith("Gateway") ? GW_SIZE : (type.contains("Event") ? EVT_SIZE : TASK_W);
                int h = type.endsWith("Gateway") ? GW_SIZE : (type.contains("Event") ? EVT_SIZE : TASK_H);
                int x = FIRST_NODE_X + (depth * NODE_X_STEP);
                int y = POOL_Y + (laneIdx * LANE_HEIGHT) + (LANE_HEIGHT / 2) - (h / 2);
                coords.put(id, new int[]{x, y, w, h});
            });

            StringBuilder di = new StringBuilder();
            di.append("\n  <bpmndi:BPMNDiagram id=\"BPMNDiagram_1\">\n");
            di.append(String.format("    <bpmndi:BPMNPlane id=\"BPMNPlane_1\" bpmnElement=\"%s\">\n", collabId));

            di.append(String.format(
                    "      <bpmndi:BPMNShape id=\"%s_di\" bpmnElement=\"%s\" isHorizontal=\"true\">\n" +
                            "        <dc:Bounds x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" />\n" +
                            "      </bpmndi:BPMNShape>\n",
                    participantId, participantId, POOL_X, POOL_Y, poolWidth, poolHeight));

            if (hasLanes) {
                for (int i = 0; i < laneOrder.size(); i++) {
                    String laneId = laneOrder.get(i);
                    di.append(String.format(
                            "      <bpmndi:BPMNShape id=\"%s_di\" bpmnElement=\"%s\" isHorizontal=\"true\">\n" +
                                    "        <dc:Bounds x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" />\n" +
                                    "      </bpmndi:BPMNShape>\n",
                            laneId, laneId, POOL_X + POOL_LABEL_W, POOL_Y + (i * LANE_HEIGHT), poolWidth - POOL_LABEL_W, LANE_HEIGHT));
                }
            }

            coords.forEach((id, c) -> di.append(String.format(
                    "      <bpmndi:BPMNShape id=\"%s_di\" bpmnElement=\"%s\">\n" +
                            "        <dc:Bounds x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" />\n" +
                            "      </bpmndi:BPMNShape>\n",
                    id, id, c[0], c[1], c[2], c[3])));

            for (FlowInfo flow : seqFlows) {
                int[] s = coords.get(flow.sourceRef);
                int[] t = coords.get(flow.targetRef);
                if (s == null || t == null) {
                    continue;
                }
                di.append(String.format(
                        "      <bpmndi:BPMNEdge id=\"%s_di\" bpmnElement=\"%s\">\n" +
                                "        <di:waypoint x=\"%d\" y=\"%d\" />\n" +
                                "        <di:waypoint x=\"%d\" y=\"%d\" />\n" +
                                "      </bpmndi:BPMNEdge>\n",
                        flow.id, flow.id, s[0] + s[2], s[1] + (s[3] / 2), t[0], t[1] + (t[3] / 2)));
            }

            di.append("    </bpmndi:BPMNPlane>\n  </bpmndi:BPMNDiagram>\n");

            String closingTag = "</bpmn:definitions>";
            int insertPos = semanticXml.lastIndexOf(closingTag);
            if (insertPos == -1) {
                closingTag = "</definitions>";
                insertPos = semanticXml.lastIndexOf(closingTag);
            }
            if (insertPos == -1) {
                log.error("Could not find closing definitions tag");
                return semanticXml;
            }

            return semanticXml.substring(0, insertPos) + di + closingTag;
        } catch (Exception e) {
            log.error("Layout injection failed", e);
            return semanticXml;
        }
    }

    private Document parseXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private String cleanXmlResponse(String raw) {
        if (raw == null) {
            return "";
        }

        String cleaned = raw.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("```(xml)?\\s*", "");
            int last = cleaned.lastIndexOf("```");
            if (last != -1) {
                cleaned = cleaned.substring(0, last).trim();
            }
        }

        int xmlStart = cleaned.indexOf("<?xml");
        if (xmlStart == -1) {
            xmlStart = cleaned.indexOf("<bpmn:definitions");
        }
        if (xmlStart == -1) {
            xmlStart = cleaned.indexOf("<definitions");
        }
        if (xmlStart > 0) {
            cleaned = cleaned.substring(xmlStart);
        }
        return cleaned;
    }

    public String repairSemanticXml(String xml) {
        if (xml == null || xml.isBlank()) {
            return "";
        }

        try {
            String repaired = cleanXmlResponse(callGroq(getSystemPrompt(), buildRepairPrompt(xml)));
            return repaired;
        } catch (Exception e) {
            log.error("Error repairing BPMN XML", e);
            return "";
        }
    }

    private boolean isValidBpmnXml(String xml) {
        if (xml == null || xml.isBlank()) {
            log.warn("BPMN validation failed: XML is empty.");
            return false;
        }

        try {
            Document doc = parseXml(xml);
            boolean hasDefinitions = doc.getElementsByTagNameNS("*", "definitions").getLength() > 0;
            boolean hasProcess = doc.getElementsByTagNameNS("*", "process").getLength() > 0;
            boolean hasStartOrTask =
                    doc.getElementsByTagNameNS("*", "startEvent").getLength() > 0 ||
                    doc.getElementsByTagNameNS("*", "task").getLength() > 0 ||
                    doc.getElementsByTagNameNS("*", "userTask").getLength() > 0 ||
                    doc.getElementsByTagNameNS("*", "serviceTask").getLength() > 0;

            if (!hasDefinitions || !hasProcess || !hasStartOrTask) {
                log.warn(
                        "BPMN validation failed: hasDefinitions={}, hasProcess={}, hasStartOrTask={}",
                        hasDefinitions,
                        hasProcess,
                        hasStartOrTask
                );
            }

            return hasDefinitions && hasProcess && hasStartOrTask;
        } catch (Exception e) {
            log.warn("Invalid BPMN XML detected: {}", e.getMessage());
            return false;
        }
    }

    private String toLogPreview(String value) {
        if (value == null || value.isBlank()) {
            return "<empty>";
        }

        String normalized = value
                .replace("\r", "\\r")
                .replace("\n", "\\n");

        if (normalized.length() <= LOG_PREVIEW_CHARS) {
            return normalized;
        }

        return normalized.substring(0, LOG_PREVIEW_CHARS) + "...[truncated]";
    }

    private AiGenerationResult failureResult(
            String error,
            String extractedTextPreview,
            String rawResponsePreview,
            String cleanedXmlPreview,
            String repairedXmlPreview
    ) {
        return new AiGenerationResult("", error, extractedTextPreview, rawResponsePreview, cleanedXmlPreview, repairedXmlPreview);
    }

    private String safeMessage(Exception e) {
        return e.getMessage() == null || e.getMessage().isBlank() ? e.getClass().getSimpleName() : e.getMessage();
    }

    public record AiGenerationResult(
            String xml,
            String error,
            String extractedTextPreview,
            String rawResponsePreview,
            String cleanedXmlPreview,
            String repairedXmlPreview
    ) {
    }

    private record FlowInfo(String id, String sourceRef, String targetRef) {
    }

    private record LocalAnalysisResponse(
            @JsonProperty("process_name") String processName,
            List<LocalActivity> activities,
            List<LocalDecision> decisions,
            List<String> departments,
            Double confidence,
            @JsonProperty("fallback_required") Boolean fallbackRequired,
            @JsonProperty("normalized_text") String normalizedText
    ) {
        public double resolvedConfidence() {
            return confidence == null ? 0.0 : confidence;
        }

        public boolean requiresFallback() {
            return Boolean.TRUE.equals(fallbackRequired);
        }

        public String toSummary() {
            return "processName=" + processName + ", activities=" + (activities == null ? 0 : activities.size())
                    + ", decisions=" + (decisions == null ? 0 : decisions.size())
                    + ", departments=" + (departments == null ? 0 : departments.size())
                    + ", confidence=" + resolvedConfidence();
        }
    }

    private record LocalActivity(
            String id,
            String label,
            String actor,
            @JsonProperty("task_type") String taskType
    ) {
    }

    private record LocalDecision(
            String id,
            String label,
            @JsonProperty("gateway_type") String gatewayType,
            @JsonProperty("condition_type") String conditionType
    ) {
    }
}
