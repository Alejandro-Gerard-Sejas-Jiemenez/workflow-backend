package com.sw.api.services;

import com.sw.api.dtos.InmuebleDTO;
import com.sw.api.dtos.InmuebleRequestDTO;
import com.sw.api.dtos.UbicacionDTO;
import com.sw.api.models.Inmueble;
import com.sw.api.models.Ubicacion;
import com.sw.api.repositories.InmuebleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class InmuebleService {

    private final InmuebleRepository repository;

    public InmuebleService(InmuebleRepository repository) {
        this.repository = repository;
    }

    public Inmueble obtenerEntidadPorId(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inmueble no encontrado"));
    }

    @Transactional
    public InmuebleDTO crear(InmuebleRequestDTO dto) {
        Inmueble inmueble = new Inmueble();
        inmueble.setTipoInmueble(dto.tipoInmueble());
        inmueble.setAreaTerreno(dto.areaTerreno());
        inmueble.setAreaConstruida(dto.areaConstruida());
        inmueble.setHabitaciones(dto.habitaciones());
        inmueble.setBanos(dto.banos());
        inmueble.setGarajes(dto.garajes());
        inmueble.setAntiguedadAnios(dto.antiguedadAnios());

        if (dto.ubicacion() != null) {
            Ubicacion ubi = new Ubicacion();
            ubi.setCiudad(dto.ubicacion().ciudad());
            ubi.setZonaBarrios(dto.ubicacion().zonaBarrios());
            ubi.setDireccionExacta(dto.ubicacion().direccionExacta());
            ubi.setLatitud(dto.ubicacion().latitud());
            ubi.setLongitud(dto.ubicacion().longitud());
            inmueble.setUbicacion(ubi);
        }

        Inmueble guardado = repository.save(inmueble);
        return mapToDTO(guardado);
    }
    
    @Transactional(readOnly = true)
    public List<InmuebleDTO> obtenerTodos() {
        return repository.findAll().stream().map(this::mapToDTO).collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public InmuebleDTO obtenerPorId(UUID id) {
        return mapToDTO(obtenerEntidadPorId(id));
    }
    
    @Transactional
    public InmuebleDTO actualizar(UUID id, InmuebleRequestDTO dto) {
        Inmueble inmueble = obtenerEntidadPorId(id);
        
        if(dto.tipoInmueble() != null) inmueble.setTipoInmueble(dto.tipoInmueble());
        if(dto.areaTerreno() != null) inmueble.setAreaTerreno(dto.areaTerreno());
        if(dto.areaConstruida() != null) inmueble.setAreaConstruida(dto.areaConstruida());
        if(dto.habitaciones() != null) inmueble.setHabitaciones(dto.habitaciones());
        if(dto.banos() != null) inmueble.setBanos(dto.banos());
        if(dto.garajes() != null) inmueble.setGarajes(dto.garajes());
        if(dto.antiguedadAnios() != null) inmueble.setAntiguedadAnios(dto.antiguedadAnios());
        
        if (dto.ubicacion() != null) {
            Ubicacion ubi = inmueble.getUbicacion();
            if (ubi == null) {
                ubi = new Ubicacion();
                inmueble.setUbicacion(ubi);
            }
            if(dto.ubicacion().ciudad() != null) ubi.setCiudad(dto.ubicacion().ciudad());
            if(dto.ubicacion().zonaBarrios() != null) ubi.setZonaBarrios(dto.ubicacion().zonaBarrios());
            if(dto.ubicacion().direccionExacta() != null) ubi.setDireccionExacta(dto.ubicacion().direccionExacta());
            if(dto.ubicacion().latitud() != null) ubi.setLatitud(dto.ubicacion().latitud());
            if(dto.ubicacion().longitud() != null) ubi.setLongitud(dto.ubicacion().longitud());
        }
        
        return mapToDTO(repository.save(inmueble));
    }
    
    @Transactional
    public void eliminar(UUID id) {
        repository.delete(obtenerEntidadPorId(id));
    }

    public InmuebleDTO mapToDTO(Inmueble inmueble) {
        if (inmueble == null) return null;
        
        UbicacionDTO ubiDTO = null;
        if (inmueble.getUbicacion() != null) {
            Ubicacion u = inmueble.getUbicacion();
            ubiDTO = new UbicacionDTO(
                    u.getId(), u.getCiudad(), u.getZonaBarrios(), 
                    u.getDireccionExacta(), u.getLatitud(), u.getLongitud()
            );
        }
        
        return new InmuebleDTO(
                inmueble.getId(),
                inmueble.getTipoInmueble(),
                inmueble.getAreaTerreno(),
                inmueble.getAreaConstruida(),
                inmueble.getHabitaciones(),
                inmueble.getBanos(),
                inmueble.getGarajes(),
                inmueble.getAntiguedadAnios(),
                ubiDTO
        );
    }
}
