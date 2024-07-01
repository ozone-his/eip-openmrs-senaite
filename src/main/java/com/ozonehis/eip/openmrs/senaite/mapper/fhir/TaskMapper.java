package com.ozonehis.eip.openmrs.senaite.mapper.fhir;

import com.ozonehis.eip.openmrs.senaite.mapper.ToFhirMapping;
import com.ozonehis.eip.openmrs.senaite.model.AnalysisRequest;
import org.hl7.fhir.r4.model.Task;

public class TaskMapper implements ToFhirMapping<Task, AnalysisRequest> {

    @Override
    public Task toFhir(AnalysisRequest analysisRequest) {
        Task task = new Task();
        task.addBasedOn().setReference(analysisRequest.getClientSampleID()).setType("ServiceRequest");
        return null;
    }
}
