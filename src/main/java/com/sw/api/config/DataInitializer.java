package com.sw.api.config;

import com.sw.api.models.Rol;
import com.sw.api.repositories.RolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RolRepository rolRepository;

    @Override
    public void run(String... args) throws Exception {
        initializeRoles();
    }

    private void initializeRoles() {
        List<String> rolesToInitialize = List.of("ROLE_ADMIN", "ROLE_EMPLEADO", "ROLE_CLIENTE", "ROLE_USER");

        rolesToInitialize.forEach(nombreRol -> {
            if (rolRepository.findByNombre(nombreRol).isEmpty()) {
                Rol nuevoRol = new Rol();
                nuevoRol.setNombre(nombreRol);
                rolRepository.save(nuevoRol);
            }
        });
    }
}
