/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.handlers.openmrs;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class TaskHandler {

    @Autowired
    private IGenericClient openmrsFhirClient;

    public void sendTask(Task task) {
        MethodOutcome methodOutcome =
                openmrsFhirClient.create().resource(task).encodedJson().execute();

        log.debug("TaskHandler: Task created {}", methodOutcome.getCreated());
    }

    public Task getTaskByServiceRequestID(String serviceRequestID) {
        Bundle bundle = openmrsFhirClient
                .search()
                .forResource(Task.class)
                .returnBundle(Bundle.class)
                .execute();

        return bundle.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(Task.class::isInstance)
                .map(Task.class::cast)
                .filter(task -> task.getBasedOn()
                        .get(0)
                        .getReference()
                        .equals(serviceRequestID)) // TODO: Use client impl and don't fetch all Tasks
                .findFirst()
                .orElse(null);
    }

    public Task updateTask(Task task, String taskID) {
        MethodOutcome methodOutcome = openmrsFhirClient.update().resource(task).execute();

        log.debug("TaskHandler: Task updateTask {}", methodOutcome.getCreated());

        return (Task) methodOutcome.getResource();
    }

    public Task markTaskRejected(Task task) {
        Task rejectTask = new Task();
        rejectTask.setId(task.getId());
        rejectTask.setStatus(Task.TaskStatus.REJECTED);
        rejectTask.setIntent(Task.TaskIntent.ORDER);
        return rejectTask;
    }

    public Task updateTaskStatus(Task task, String analysisRequestTaskStatus) {
        Task updateTask = new Task();
        updateTask.setId(task.getIdPart());
        updateTask.setIntent(Task.TaskIntent.ORDER);
        updateTask.setStatus(Task.TaskStatus.fromCode(analysisRequestTaskStatus));
        return updateTask;
    }

    public boolean doesTaskExists(Task task) {
        return task != null && task.hasId() && task.hasStatus();
    }
}
