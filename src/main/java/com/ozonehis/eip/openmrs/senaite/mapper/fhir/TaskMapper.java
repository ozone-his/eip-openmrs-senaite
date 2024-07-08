package com.ozonehis.eip.openmrs.senaite.mapper.fhir;

import com.ozonehis.eip.openmrs.senaite.mapper.ToFhirMapping;
import com.ozonehis.eip.openmrs.senaite.model.analysisRequest.AnalysisRequest;
import lombok.Setter;
import org.hl7.fhir.r4.model.Task;
import org.springframework.stereotype.Component;

@Setter
@Component
public class TaskMapper implements ToFhirMapping<Task, AnalysisRequest> {

    @Override
    public Task toFhir(AnalysisRequest analysisRequest) {
        Task task = new Task();
        task.setIntent(Task.TaskIntent.ORDER);
        task.addBasedOn().setReference(analysisRequest.getClientSampleID()).setType("ServiceRequest");
        return null;
    }
}
