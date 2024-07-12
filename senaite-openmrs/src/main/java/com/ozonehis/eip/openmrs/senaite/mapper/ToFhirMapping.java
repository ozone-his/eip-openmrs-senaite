/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.mapper;

import org.hl7.fhir.r4.model.Resource;

/**
 * An Interface for mapping from FHIR Resources to SENAITE Resource
 *
 * @param <F> FHIR Resource
 * @param <E> SENAITE Resource
 */
public interface ToFhirMapping<F extends Resource, E> {

    /**
     * Maps an SENAITE Resource to a FHIR Resource
     *
     * @param senaiteResource SENAITE Resource
     * @return FHIR Resource
     */
    F toFhir(E senaiteResource);
}
