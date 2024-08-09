/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.mapper;

import com.ozonehis.eip.openmrs.senaite.model.SenaiteResource;

/**
 * An Interface for mapping from FHIR Resources to SENAITE resource
 *
 * @param <F> FHIR Resource
 * @param <E> SENAITE resource
 */
public interface ToSenaiteMapping<F, E extends SenaiteResource> {

    /**
     * Maps a FHIR Resource to an SENAITE resource
     *
     * @param fhirResource FHIR Resource
     * @return SENAITE resource
     */
    E toSenaite(F fhirResource);
}
