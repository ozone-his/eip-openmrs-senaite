/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.handlers.openmrs;

import ca.uhn.fhir.context.FhirContext;
import com.ozonehis.eip.openmrs.senaite.Constants;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Task;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class TaskHandler {

    public Task sendTask(ProducerTemplate producerTemplate, Task task) {
        String response = producerTemplate.requestBody("direct:openmrs-create-task-route", task, String.class);
        log.info("sendTask response {}", response);
        FhirContext ctx = FhirContext.forR4();
        Task savedTask = ctx.newJsonParser().parseResource(Task.class, response);
        log.info("sendTask {}", savedTask);
        return savedTask;
    }

    public Task getTaskByServiceRequestID(ProducerTemplate producerTemplate, String serviceRequestID) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.HEADER_SERVICE_REQUEST_ID, serviceRequestID);
        String response =
                producerTemplate.requestBodyAndHeaders("direct:openmrs-get-task-route", null, headers, String.class);
        log.info("getTask response {}", response);
        FhirContext ctx = FhirContext.forR4();
        Bundle bundle = ctx.newJsonParser().parseResource(Bundle.class, response);
        List<Bundle.BundleEntryComponent> entries = bundle.getEntry();

        Task task = null;
        for (Bundle.BundleEntryComponent entry : entries) {
            Resource resource = entry.getResource();
            if (resource instanceof Task) {
                task = (Task) resource;
            }
        }
        log.info("getTask {}", task);
        return task;
    }

    public Task updateTask(ProducerTemplate producerTemplate, Task task, String taskID) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.HEADER_TASK_ID, taskID);
        String response =
                producerTemplate.requestBodyAndHeaders("direct:openmrs-update-task-route", task, headers, String.class);
        log.info("updateTask response {}", response);
        FhirContext ctx = FhirContext.forR4();
        Task updatedTask = ctx.newJsonParser().parseResource(Task.class, response);
        log.info("updateTask {}", updatedTask);
        return updatedTask;
    }

    public Task markTaskRejected(ProducerTemplate producerTemplate, Task task) {
        Task rejectTask = new Task();
        rejectTask.setId(task.getId());
        rejectTask.setStatus(Task.TaskStatus.REJECTED);
        rejectTask.setIntent(Task.TaskIntent.ORDER);
        return updateTask(producerTemplate, rejectTask, task.getIdPart());
    }

    public Task updateTaskStatus(ProducerTemplate producerTemplate, Task task, String analysisRequestTaskStatus) {
        Task updateTask = new Task();
        updateTask.setId(task.getIdPart());
        updateTask.setIntent(Task.TaskIntent.ORDER);
        updateTask.setStatus(Task.TaskStatus.fromCode(analysisRequestTaskStatus));
        log.info(
                "TaskProcessor: Updating Task with id {} from status {} to status {} analysisRequest {}",
                task.getIdPart(),
                task.getStatus().toString(),
                Task.TaskStatus.fromCode(analysisRequestTaskStatus),
                analysisRequestTaskStatus);
        return updateTask(producerTemplate, updateTask, task.getIdPart());
    }
}
