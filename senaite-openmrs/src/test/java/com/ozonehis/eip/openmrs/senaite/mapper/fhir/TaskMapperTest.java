/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.mapper.fhir;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.MockitoAnnotations.openMocks;

import com.ozonehis.eip.openmrs.senaite.model.analysisRequest.AnalysisRequestDTO;
import org.hl7.fhir.r4.model.Task;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

class TaskMapperTest {

    @InjectMocks
    private TaskMapper taskMapper;

    private static AutoCloseable mocksCloser;

    @BeforeEach
    void setUp() {
        mocksCloser = openMocks(this);
    }

    @AfterAll
    static void close() throws Exception {
        mocksCloser.close();
    }

    @Test
    void shouldReturnTaskGivenAnalysisRequest() {
        // Setup
        AnalysisRequestDTO analysisRequestDTO = new AnalysisRequestDTO();
        analysisRequestDTO.setClientSampleID("SampleID123");

        // Act
        Task task = taskMapper.toFhir(analysisRequestDTO);

        // Assert
        assertNotNull(task);
        assertEquals(Task.TaskIntent.ORDER, task.getIntent());
        assertNotNull(task.getBasedOnFirstRep());
        assertEquals("SampleID123", task.getBasedOnFirstRep().getReference());
        assertEquals("ServiceRequest", task.getBasedOnFirstRep().getType());
    }
}
