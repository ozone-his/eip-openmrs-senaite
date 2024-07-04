package com.ozonehis.eip.openmrs.senaite.converters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ozonehis.eip.openmrs.senaite.model.Contact;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Converter;

@Slf4j
@Converter
public class ContactConverter {

    @Converter
    public static InputStream convertContactToInputStream(Contact contact) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(contact);
        return new ByteArrayInputStream(json.getBytes());
    }
}
