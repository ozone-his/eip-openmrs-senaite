package com.ozonehis.eip.openmrs.senaite.converters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ozonehis.eip.openmrs.senaite.model.Client;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Converter;
import org.springframework.stereotype.Component;

@Slf4j
@Converter
@Component
public class ClientConverter {

    @Converter
    public static InputStream convertClientToInputStream(Client client) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(client);
        return new ByteArrayInputStream(json.getBytes());
    }
}
