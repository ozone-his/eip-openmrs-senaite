/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.routes.analysisrequest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AnalysisRequestCatalogQueryTest {

    @Test
    void shouldQueryPublishedSamplesFromAWatermark() {
        String query = AnalysisRequestCatalogQuery.publishedSince("2026-10-28T14:00:00", 20);

        assertTrue(query.contains("catalog=senaite_catalog_sample"));
        assertTrue(query.contains("review_state=published"));
        assertTrue(query.contains("sort_on=getDatePublished"));
        assertTrue(query.contains("sort_order=ascending"));
        assertTrue(query.contains("limit=50"));
        assertTrue(query.contains("b_start=20"));
        assertTrue(query.contains("getDatePublished.query:record=2026-10-28T14%3A00%3A00"));
        assertTrue(query.contains("getDatePublished.range:record=min"));
        assertFalse(query.contains("modified"));
    }

    @Test
    void shouldQueryReceivedSamplesFromAWatermark() {
        String query = AnalysisRequestCatalogQuery.receivedSince("2026-10-20T00:00:00", 0);

        assertTrue(query.contains("review_state=sample_received"));
        assertTrue(query.contains("sort_on=getDateReceived"));
        assertTrue(query.contains("sort_order=ascending"));
        assertTrue(query.contains("b_start=0"));
        assertTrue(query.contains("getDateReceived.query:record=2026-10-20T00%3A00%3A00"));
        assertTrue(query.contains("getDateReceived.range:record=min"));
    }
}
