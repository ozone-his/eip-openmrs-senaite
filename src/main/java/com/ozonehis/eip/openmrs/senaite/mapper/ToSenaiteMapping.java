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
