package com.sw.api.repositories;

import com.sw.api.models.Bitacora;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BitacoraRepository extends MongoRepository<Bitacora, String> {
    List<Bitacora> findAllByOrderByFechaDesc();
}
