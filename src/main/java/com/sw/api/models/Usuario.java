package com.sw.api.models;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Getter @Setter
@Document(collection = "usuarios")
public class Usuario extends Auditable implements UserDetails {

    @Id
    private String id;

    private String nombre;

    private String apellido;

    private String email;

    private String password;

    private String departamento;

    private String telefono;

    private boolean activo = true;

    private boolean estadoConexion = false;

    private LocalDateTime ultimaConexion;
    
    @DBRef
    private Rol rol;
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String roleName = "ROLE_USER";
        if (rol != null && rol.getNombre() != null) {
            roleName = rol.getNombre();
        }
        return List.of(new SimpleGrantedAuthority(roleName));
    }

    @Override
    public String getPassword() {
        return this.password; 
    }

    @Override
    public String getUsername() {
        return this.email; 
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return this.activo; }
}