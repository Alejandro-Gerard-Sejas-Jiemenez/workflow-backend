package com.sw.api.config;

import com.sw.api.models.Formulario;
import com.sw.api.models.Rol;
import com.sw.api.models.Usuario;
import com.sw.api.models.Workflow;
import com.sw.api.repositories.FormularioRepository;
import com.sw.api.repositories.RolRepository;
import com.sw.api.repositories.UsuarioRepository;
import com.sw.api.repositories.WorkflowRepository;
import com.sw.api.repositories.DepartamentoRepository;
import com.sw.api.models.Departamento;
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
    private final DepartamentoRepository departamentoRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        initializeRoles();
        initializeDepartamentos();
        initializeAdmin();
        initializeTestData();
    }

    private void initializeRoles() {
        List<String> rolesToInitialize = List.of("ROLE_ADMIN", "ROLE_EMPLEADO", "ROLE_CLIENTE", "ROLE_USER",
                "ROLE_DESIGNER");

        rolesToInitialize.forEach(nombreRol -> {
            if (rolRepository.findByNombre(nombreRol).isEmpty()) {
                Rol nuevoRol = new Rol();
                nuevoRol.setNombre(nombreRol);
                rolRepository.save(nuevoRol);
            }
        });
    }

    private void initializeDepartamentos() {
        if (departamentoRepository.count() == 0) {
            List<String> deps = List.of("TI", "Recursos Humanos", "Finanzas", "Operaciones", "Atención al Cliente",
                    "Gerencia", "RECEPCION");
            deps.forEach(nombre -> {
                Departamento d = new Departamento();
                d.setNombre(nombre);
                departamentoRepository.save(d);
            });
            System.out.println("✅ Departamentos inicializados por defecto.");
        }
    }

    private void initializeAdmin() {
        String adminEmail = "admin@workflow.com";
        if (usuarioRepository.findByEmail(adminEmail).isEmpty()) {
            Rol adminRol = rolRepository.findByNombre("ROLE_ADMIN")
                    .orElseThrow(() -> new RuntimeException("Error: Rol ROLE_ADMIN no encontrado."));

            Usuario admin = new Usuario();
            admin.setNombre("Admin");
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setDepartamentos(List.of("TI"));
            admin.setRol(adminRol);
            admin.setActivo(true);

            usuarioRepository.save(admin);
            System.out.println("✅ Usuario Administrador creado: " + adminEmail);
        }

        String employeeEmail = "empleado@workflow.com";
        if (usuarioRepository.findByEmail(employeeEmail).isEmpty()) {
            Rol empRol = rolRepository.findByNombre("ROLE_EMPLEADO")
                    .orElseThrow(() -> new RuntimeException("Error: Rol ROLE_EMPLEADO no encontrado."));

            Usuario employee = new Usuario();
            employee.setNombre("Funcionario Prueba");
            employee.setEmail(employeeEmail);
            employee.setPassword(passwordEncoder.encode("empleado123"));
            employee.setDepartamentos(List.of("RECEPCION"));
            employee.setRol(empRol);
            employee.setActivo(true);

            usuarioRepository.save(employee);
            System.out.println("✅ Usuario Funcionario creado: " + employeeEmail);
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
                    new Formulario.Campo("comentarios", "text", false, null)));
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
                workflowVacaciones.setEstado("PUBLICADO");
                workflowRepository.save(workflowVacaciones);
                System.out.println("✅ Workflow de Vacaciones creado.");
            }
        }
    }
}
