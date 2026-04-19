package com.sw.api.config;

import com.sw.api.models.Formulario;
import com.sw.api.models.Rol;
import com.sw.api.models.Usuario;
import com.sw.api.models.Workflow;
import com.sw.api.repositories.FormularioRepository;
import com.sw.api.repositories.RolRepository;
import com.sw.api.repositories.UsuarioRepository;
import com.sw.api.repositories.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final FormularioRepository formularioRepository;
    private final WorkflowRepository workflowRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        initializeRoles();
        initializeAdmin();
        initializeTestData();
    }

    private void initializeRoles() {
        List<String> rolesToInitialize = List.of("ROLE_ADMIN", "ROLE_EMPLEADO", "ROLE_CLIENTE", "ROLE_USER", "ROLE_DESIGNER");

        rolesToInitialize.forEach(nombreRol -> {
            if (rolRepository.findByNombre(nombreRol).isEmpty()) {
                Rol nuevoRol = new Rol();
                nuevoRol.setNombre(nombreRol);
                rolRepository.save(nuevoRol);
            }
        });
    }

    private void initializeAdmin() {
        String adminEmail = "admin@workflow.com";
        if (usuarioRepository.findByEmail(adminEmail).isEmpty()) {
            Rol adminRol = rolRepository.findByNombre("ROLE_ADMIN")
                    .orElseThrow(() -> new RuntimeException("Error: Rol ROLE_ADMIN no encontrado."));

            Usuario admin = new Usuario();
            admin.setNombre("Admin");
            admin.setApellido("Sistema");
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setDepartamento("TI");
            admin.setRol(adminRol);
            admin.setActivo(true);

            usuarioRepository.save(admin);
            System.out.println("✅ Usuario Administrador creado: " + adminEmail);
        }
    }

    private void initializeTestData() {
        // 1. Crear Formulario de Vacaciones si no existe
        if (formularioRepository.findAll().isEmpty()) {
            Formulario formVacaciones = new Formulario();
            formVacaciones.setNombre("Solicitud de Vacaciones");
            formVacaciones.setCampos(List.of(
                new Formulario.Campo("fecha_inicio", "date", true, null),
                new Formulario.Campo("fecha_fin", "date", true, null),
                new Formulario.Campo("motivo", "text", true, null),
                new Formulario.Campo("comentarios", "text", false, null)
            ));
            formVacaciones = formularioRepository.save(formVacaciones);
            System.out.println("✅ Formulario de Vacaciones creado.");

            // 2. Crear Workflow de Vacaciones
            if (workflowRepository.findAll().isEmpty()) {
                Workflow workflowVacaciones = new Workflow();
                workflowVacaciones.setNombre("Gestion de Vacaciones");
                workflowVacaciones.setDescripcion("Proceso estandar para la solicitud y aprobacion de dias libres.");
                
                List<Workflow.Paso> pasos = new ArrayList<>();
                pasos.add(new Workflow.Paso("Solicitud", 1, "CLIENTE", formVacaciones.getId()));
                pasos.add(new Workflow.Paso("Revision RRHH", 2, "EMPLEADO", formVacaciones.getId()));
                pasos.add(new Workflow.Paso("Aprobacion Final", 3, "ADMIN", formVacaciones.getId()));
                
                workflowVacaciones.setPasos(pasos);
                workflowRepository.save(workflowVacaciones);
                System.out.println("✅ Workflow de Vacaciones creado.");
            }
        }
    }
}
