package com.ozonehis.eip.openmrs.senaite.handlers.senaite;

import com.ozonehis.eip.openmrs.senaite.client.SenaiteClient;
import com.ozonehis.eip.openmrs.senaite.model.AnalysisRequest;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class AnalysisRequestHandler {

    @Autowired
    private SenaiteClient senaiteClient;

    public AnalysisRequest getAnalysisRequestIfExists(String serviceRequestUuid, String patientId) {

        return null;
    }
}
