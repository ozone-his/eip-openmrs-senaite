package com.ozonehis.eip.openmrs.senaite.client;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Getter
@Component
public class OpenmrsFhirClient {
    @Value("${openmrs.username}")
    private String openmrsUsername;

    @Value("${openmrs.password}")
    private String openmrsPassword;

    @Value("${fhirR4.baseUrl}")
    private String openmrsFhirBaseUrl;

    public String authHeader() {
        String auth = getOpenmrsUsername() + ":" + getOpenmrsPassword();
        byte[] encodedAuth = Base64.encodeBase64(auth.getBytes());
        return "Basic " + new String(encodedAuth);
    }
}
