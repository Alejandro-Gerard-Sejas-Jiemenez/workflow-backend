package com.sw.api.workflow.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sw.api.formulario.models.Formulario;
import com.sw.api.formulario.repositories.FormularioRepository;
import com.sw.api.workflow.models.Workflow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WorkflowDiagramSyncService {

    private final ObjectMapper objectMapper;
    private final FormularioRepository formularioRepository;

    public void syncPasosDesdeDiagrama(Workflow workflow) {
        String diagramData = workflow.getDiagramData();
        if (diagramData == null || diagramData.isBlank()) {
            return;
        }

        try {
            System.out.println("🔍 [DEBUG] Iniciando sincronizacion: " + workflow.getNombre());
            JsonNode root = objectMapper.readTree(diagramData);
            JsonNode nodes = root.get("nodes");
            System.out.println("   [DEBUG] Total nodos en el JSON del diagrama: " + (nodes != null ? nodes.size() : 0));
            
            if (nodes == null || !nodes.isArray()) {
                System.out.println("⚠️ [DEBUG] El diagrama no tiene un array de nodos valido.");
                return;
            }

            List<Workflow.Paso> nuevosPasos = new ArrayList<>();
            Map<String, String> nodeNames = new HashMap<>();
            
            // 1. Mapear nombres de nodos para herencia de departamentos
            for (JsonNode node : nodes) {
                if (node.has("id") && node.has("data") && node.get("data").has("label")) {
                    nodeNames.put(node.get("id").asText(), node.get("data").get("label").asText());
                }
            }

            List<JsonNode> lanes = new ArrayList<>();
            for (JsonNode n : nodes) {
                String type = n.has("type") ? n.get("type").asText() : "";
                if ("lane".equals(type)) {
                    lanes.add(n);
                    String label = n.has("data") && n.get("data").has("label") ? n.get("data").get("label").asText() : "Sin etiqueta";
                    double lx = n.has("position") && n.get("position").has("x") ? n.get("position").get("x").asDouble() : 0.0;
                    double ly = n.has("position") && n.get("position").has("y") ? n.get("position").get("y").asDouble() : 0.0;
                    double lw = n.has("size") && n.get("size").has("width") ? n.get("size").get("width").asDouble() : 200.0;
                    double lh = n.has("size") && n.get("size").has("height") ? n.get("size").get("height").asDouble() : 800.0;
                    System.out.println("   [DEBUG] Carril detectado: [" + label + "] -> Pos(" + lx + ", " + ly + "), Size(" + lw + ", " + lh + ")");
                }
            }

            // 2. Filtrar y ordenar los nodos: primero los nodos de tipo "start", luego los "task" (ordenados de izquierda a derecha por coordenada X)
            List<JsonNode> nodosFiltrados = new ArrayList<>();
            List<JsonNode> startNodes = new ArrayList<>();
            List<JsonNode> taskNodes = new ArrayList<>();

            for (JsonNode node : nodes) {
                String type = node.has("type") ? node.get("type").asText() : "unknown";
                if ("start".equals(type)) {
                    startNodes.add(node);
                } else if ("task".equals(type)) {
                    taskNodes.add(node);
                }
            }

            // Ordenar tareas de izquierda a derecha por posición X
            taskNodes.sort((a, b) -> {
                double ax = a.has("position") && a.get("position").has("x") ? a.get("position").get("x").asDouble() : 0.0;
                double bx = b.has("position") && b.get("position").has("x") ? b.get("position").get("x").asDouble() : 0.0;
                return Double.compare(ax, bx);
            });

            nodosFiltrados.addAll(startNodes);
            nodosFiltrados.addAll(taskNodes);

            int orden = 1;
            for (JsonNode node : nodosFiltrados) {
                String type = node.has("type") ? node.get("type").asText() : "unknown";
                
                JsonNode data = node.get("data");
                if (data == null) continue;

                String label = data.has("label") ? data.get("label").asText() : "Paso " + orden;
                    
                    // Detectar si tiene formulario (check manual o existencia de campos)
                    boolean tieneSchema = data.has("formSchema");
                    boolean tieneFields = tieneSchema && data.get("formSchema").has("fields") && data.get("formSchema").get("fields").size() > 0;
                    boolean formActivo = true;
                    if (data.has("formEnabled")) {
                        formActivo = data.get("formEnabled").asBoolean();
                    }

                    if (formActivo) {
                        System.out.println("📍 [DEBUG] Nodo procesado: " + label + " (Campos: " + (tieneFields ? data.get("formSchema").get("fields").size() : 0) + ")");
                        
                        // Determinar departamento
                        String dept = null;
                        
                        if ("start".equals(type)) {
                            dept = "RECEPCION";
                            System.out.println("   [DEBUG] Nodo de inicio. Asignando automaticamente a RECEPCION.");
                        } else {
                            // 1. Detectar si está dentro de algún carril (lane) por coordenadas
                            if (node.has("position")) {
                                double taskX = node.get("position").has("x") ? node.get("position").get("x").asDouble() : 0.0;
                                double taskY = node.get("position").has("y") ? node.get("position").get("y").asDouble() : 0.0;
                                System.out.println("   [DEBUG] Evaluando posicion de Tarea [" + label + "]: Pos(" + taskX + ", " + taskY + ")");
                                
                                for (JsonNode lane : lanes) {
                                    if (lane.has("position")) {
                                        double laneX = lane.get("position").has("x") ? lane.get("position").get("x").asDouble() : 0.0;
                                        double laneY = lane.get("position").has("y") ? lane.get("position").get("y").asDouble() : 0.0;
                                        double laneW = lane.has("size") && lane.get("size").has("width") ? lane.get("size").get("width").asDouble() : 200.0;
                                        double laneH = lane.has("size") && lane.get("size").has("height") ? lane.get("size").get("height").asDouble() : 800.0;
                                        
                                        boolean inX = taskX >= laneX && taskX <= (laneX + laneW);
                                        boolean inY = taskY >= laneY && taskY <= (laneY + laneH);
                                        
                                        if (inX && inY) {
                                            if (lane.has("data") && lane.get("data").has("label")) {
                                                dept = lane.get("data").get("label").asText();
                                                System.out.println("      [DEBUG] ¡Colision! Tarea [" + label + "] cae dentro de carril [" + dept + "]");
                                                if (dept != null && !dept.isBlank()) {
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        // 2. Si no se encontró carril por coordenadas, intentar parentId
                        if ((dept == null || dept.isBlank()) && node.has("parentId")) {
                            String parentName = nodeNames.get(node.get("parentId").asText());
                            if (parentName != null && !parentName.isBlank()) {
                                dept = parentName;
                            }
                        }
                        
                        // 3. Fallbacks a atributos directos
                        if (dept == null || dept.isBlank()) {
                            if (data.has("role") && !data.get("role").asText().isBlank()) {
                                dept = data.get("role").asText();
                            } else if (data.has("departamento") && !data.get("departamento").asText().isBlank()) {
                                dept = data.get("departamento").asText();
                            } else if (data.has("lane") && !data.get("lane").asText().isBlank()) {
                                dept = data.get("lane").asText();
                            }
                        }
                        
                        // 4. Default global
                        if (dept == null || dept.isBlank()) {
                            dept = "RECEPCION";
                        }
                        
                        dept = dept.replace("ROLE_", "");

                        // Crear o actualizar formulario (Incluso si esta vacio, para evitar el mensaje de error)
                        Formulario formulario = new Formulario();
                        String title = label;
                        boolean allowAttachments = false;
                        String allowedTypes = "";
                        String requiredDocs = "";
                        String description = "";
                        
                        if (data.has("formSchema")) {
                            JsonNode schemaNode = data.get("formSchema");
                            if (schemaNode.has("title") && !schemaNode.get("title").asText().isBlank()) {
                                title = schemaNode.get("title").asText();
                            }
                            if (schemaNode.has("allowAttachments")) {
                                allowAttachments = schemaNode.get("allowAttachments").asBoolean();
                            }
                            if (schemaNode.has("allowedTypes")) {
                                allowedTypes = schemaNode.get("allowedTypes").asText();
                            }
                            if (schemaNode.has("requiredDocs")) {
                                requiredDocs = schemaNode.get("requiredDocs").asText();
                            }
                            if (schemaNode.has("description")) {
                                description = schemaNode.get("description").asText();
                            }
                        }
                        
                        formulario.setNombre(title);
                        formulario.setAllowAttachments(allowAttachments);
                        formulario.setAllowedTypes(allowedTypes);
                        formulario.setRequiredDocs(requiredDocs);
                        formulario.setDescripcion(description);

                        List<Formulario.Campo> campos = new ArrayList<>();
                        
                        if (tieneFields) {
                            for (JsonNode field : data.get("formSchema").get("fields")) {
                                String fNombre = field.has("label") ? field.get("label").asText() : (field.has("id") ? field.get("id").asText() : "campo");
                                String fTipo = field.has("type") ? field.get("type").asText() : "text";
                                boolean fReq = field.has("required") && field.get("required").asBoolean();
                                
                                List<String> fOpciones = new ArrayList<>();
                                if (field.has("options") && field.get("options").isArray()) {
                                    for (JsonNode opt : field.get("options")) {
                                        fOpciones.add(opt.asText());
                                    }
                                }
                                campos.add(new Formulario.Campo(fNombre, fTipo, fReq, fOpciones));
                            }
                        } else if (!allowAttachments) {
                            // Campo por defecto para que no este totalmente vacio si la IA fallo y no hay adjuntos
                            campos.add(new Formulario.Campo("Observaciones", "textarea", false, new ArrayList<>()));
                        }
                        
                        formulario.setCampos(campos);
                        formulario = formularioRepository.save(formulario);
                        String formularioId = formulario.getId();
                        System.out.println("   ✅ Formulario listo: " + formularioId + " asignado a " + dept + " (Adjuntos: " + allowAttachments + ")");
                        
                        nuevosPasos.add(new Workflow.Paso(label, orden++, dept, formularioId));
                    }
                }

            workflow.setPasos(nuevosPasos);
            System.out.println("🚀 [DEBUG] Sincronizacion terminada. Pasos: " + nuevosPasos.size());

        } catch (Exception e) {
            System.err.println("❌ [DEBUG] Error en sincronizacion: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
