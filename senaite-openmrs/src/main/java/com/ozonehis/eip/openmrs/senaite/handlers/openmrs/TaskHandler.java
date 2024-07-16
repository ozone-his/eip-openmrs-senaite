/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.handlers.openmrs;

import ca.uhn.fhir.context.FhirContext;
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

    public Task getTask(ProducerTemplate producerTemplate, Map<String, Object> headers) {
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

    public Task updateTask(ProducerTemplate producerTemplate, Task task, Map<String, Object> headers) {
        String response =
                producerTemplate.requestBodyAndHeaders("direct:openmrs-update-task-route", task, headers, String.class);
        log.info("updateTask response {}", response);
        FhirContext ctx = FhirContext.forR4();
        Task updatedTask = ctx.newJsonParser().parseResource(Task.class, response);
        log.info("updateTask {}", updatedTask);
        return updatedTask;
    }
}
