/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.mapper.senaite;

import com.ozonehis.eip.openmrs.senaite.mapper.ToSenaiteMapping;
import com.ozonehis.eip.openmrs.senaite.model.client.Client;
import java.util.Optional;
import lombok.Setter;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

@Setter
@Component
public class ClientMapper implements ToSenaiteMapping<Patient, Client> {

    @Override
    public Client toSenaite(Patient patient) {
        if (patient == null) {
            return null;
        }
        Client client = new Client();
        client.setPortalType("Client");

        String patientName = getPatientName(patient).orElse("");
        String patientIdentifier = getPreferredPatientIdentifier(patient).orElse("");

        client.setTitle(String.format("%s (%s)", patientName, patientIdentifier));
        client.setClientID(patient.getIdPart());
        client.setParentPath("/senaite/clients");

        return client;
    }

    protected Optional<String> getPreferredPatientIdentifier(Patient patient) {
        return patient.getIdentifier().stream()
                .filter(identifier -> identifier.getUse() == Identifier.IdentifierUse.OFFICIAL)
                .findFirst()
                .map(Identifier::getValue);
    }

    protected Optional<String> getPatientName(Patient patient) {
        return patient.getName().stream()
                .findFirst()
                .map(name -> name.getGiven().get(0) + " " + name.getFamily());
    }
}
