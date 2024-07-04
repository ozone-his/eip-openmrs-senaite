package com.ozonehis.eip.openmrs.openmrs.handlers.openmrs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.r4.model.Task;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class TaskHandler {

    public Task sendTask(ProducerTemplate producerTemplate, Task task) throws JsonProcessingException {
        String response = producerTemplate.requestBody("direct:openmrs-create-task-route", task, String.class);
        log.error("sendTask response {}", response);
        ObjectMapper objectMapper = new ObjectMapper();
        Task savedTask = objectMapper.readValue(response, Task.class);
        log.error("sendTask {}", response);
        return savedTask;
    }

    public Task getTask(ProducerTemplate producerTemplate, String queryParams) throws JsonProcessingException {
        String response = producerTemplate.requestBody("direct:openmrs-get-task-route", null, String.class);
        log.error("getTask response {}", response);
        ObjectMapper objectMapper = new ObjectMapper();
        Task task = objectMapper.readValue(response, Task.class);
        log.error("getTask {}", task);
        return task;
    }
}
