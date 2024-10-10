/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.handlers.openmrs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICreate;
import ca.uhn.fhir.rest.gclient.ICreateTyped;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.IUntypedQuery;
import ca.uhn.fhir.rest.gclient.IUpdate;
import ca.uhn.fhir.rest.gclient.IUpdateTyped;
import java.util.Collections;
import java.util.UUID;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.Task;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

class TaskHandlerTest {

    @Mock
    private IGenericClient openmrsFhirClient;

    @Mock
    private ICreate iCreate;

    @Mock
    private ICreateTyped iCreateTyped;

    @Mock
    private IUpdate iUpdate;

    @Mock
    private IUpdateTyped iUpdateTyped;

    @Mock
    private IUntypedQuery iUntypedQuery;

    @Mock
    private IQuery iQuery;

    @InjectMocks
    private TaskHandler taskHandler;

    private static AutoCloseable mocksCloser;

    @AfterAll
    public static void close() throws Exception {
        mocksCloser.close();
    }

    @BeforeEach
    public void setup() {
        mocksCloser = openMocks(this);
    }

    @Test
    void shouldSaveTask() {
        // Setup
        String taskID = UUID.randomUUID().toString();
        Task task = new Task();
        task.setId(taskID);

        MethodOutcome methodOutcome = new MethodOutcome();
        methodOutcome.setResource(task);
        methodOutcome.setCreated(true);

        // Mock behavior
        when(openmrsFhirClient.create()).thenReturn(iCreate);
        when(iCreate.resource(task)).thenReturn(iCreateTyped);
        when(iCreateTyped.encodedJson()).thenReturn(iCreateTyped);
        when(iCreateTyped.execute()).thenReturn(methodOutcome);

        // Act
        taskHandler.sendTask(task);

        // Verify
        verify(openmrsFhirClient, times(1)).create();
        verify(iCreate, times(1)).resource(task);
        verify(iCreateTyped, times(1)).encodedJson();
        verify(iCreateTyped, times(1)).execute();
    }

    @Test
    void shouldReturnTaskGivenServiceRequestID() {
        // Setup
        String serviceRequestID = UUID.randomUUID().toString();
        String taskID = UUID.randomUUID().toString();
        Task task = new Task();
        task.setId(taskID);
        task.setBasedOn(Collections.singletonList(new Reference().setReference(serviceRequestID)));

        Bundle bundle = new Bundle();
        Bundle.BundleEntryComponent bundleEntryComponent = new Bundle.BundleEntryComponent();
        bundleEntryComponent.setResource(task);
        bundle.setEntry(Collections.singletonList(bundleEntryComponent));

        // Mock behavior
        when(openmrsFhirClient.search()).thenReturn(iUntypedQuery);
        when(iUntypedQuery.forResource(Task.class)).thenReturn(iQuery);
        when(iQuery.returnBundle(Bundle.class)).thenReturn(iQuery);
        when(iQuery.execute()).thenReturn(bundle);

        // Act
        Task result = taskHandler.getTaskByServiceRequestID(serviceRequestID);

        // Verify
        assertNotNull(result);
        assertEquals(result.getResourceType(), ResourceType.Task);
        assertEquals(result.getId(), taskID);
    }

    @Test
    void shouldReturnUpdatedTask() {
        // Setup
        String taskID = UUID.randomUUID().toString();
        Task task = new Task();
        task.setId(taskID);

        MethodOutcome methodOutcome = new MethodOutcome();
        methodOutcome.setResource(task);
        methodOutcome.setCreated(true);

        // Mock behavior
        when(openmrsFhirClient.update()).thenReturn(iUpdate);
        when(iUpdate.resource(task)).thenReturn(iUpdateTyped);
        when(iUpdateTyped.execute()).thenReturn(methodOutcome);

        // Act
        Task result = taskHandler.updateTask(task, taskID);

        // Verify
        assertNotNull(result);
        assertEquals(result.getResourceType(), ResourceType.Task);
        assertEquals(result.getId(), taskID);
    }

    @Test
    void shouldReturnRejectedTask() {
        // Setup
        String taskID = UUID.randomUUID().toString();
        Task task = new Task();
        task.setStatus(Task.TaskStatus.ACCEPTED);
        task.setId(taskID);

        // Act
        Task result = taskHandler.markTaskRejected(task);

        // Verify
        assertNotNull(result);
        assertEquals(result.getStatus(), Task.TaskStatus.REJECTED);
        assertEquals(result.getId(), taskID);
    }

    @Test
    void shouldReturnUpdatedTaskGivenTaskAndStatus() {
        // Setup
        String taskID = UUID.randomUUID().toString();
        Task task = new Task();
        task.setStatus(Task.TaskStatus.RECEIVED);
        task.setId(taskID);

        // Act
        Task result = taskHandler.updateTaskStatus(task, "completed");

        // Verify
        assertNotNull(result);
        assertEquals(result.getStatus(), Task.TaskStatus.COMPLETED);
        assertEquals(result.getId(), taskID);
    }
}
