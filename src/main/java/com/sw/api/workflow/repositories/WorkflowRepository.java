package com.sw.api.workflow.repositories;

import com.sw.api.workflow.models.Workflow;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowRepository extends MongoRepository<Workflow, String> {
    List<Workflow> findByOwnerUserIdOrCollaboratorsUserId(String ownerUserId, String collaboratorUserId);
    List<Workflow> findByEstado(String estado);
}
