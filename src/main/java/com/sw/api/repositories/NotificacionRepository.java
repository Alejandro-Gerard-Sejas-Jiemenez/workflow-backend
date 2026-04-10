package com.sw.api.repositories;

import com.sw.api.models.Notificacion;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificacionRepository extends MongoRepository<Notificacion, String> {
}
