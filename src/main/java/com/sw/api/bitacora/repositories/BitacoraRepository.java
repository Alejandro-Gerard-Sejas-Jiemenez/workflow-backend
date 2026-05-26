package com.sw.api.bitacora.repositories;

import com.sw.api.bitacora.models.Bitacora;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BitacoraRepository extends MongoRepository<Bitacora, String> {
    List<Bitacora> findAllByOrderByFechaDesc();
}
