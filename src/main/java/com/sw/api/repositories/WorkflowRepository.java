package com.sw.api.repositories;

import com.sw.api.models.Workflow;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkflowRepository extends MongoRepository<Workflow, String> {
}
