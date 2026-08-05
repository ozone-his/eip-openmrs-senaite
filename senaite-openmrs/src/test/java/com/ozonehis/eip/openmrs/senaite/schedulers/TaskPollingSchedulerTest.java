/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.schedulers;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.openMocks;

import org.apache.camel.ProducerTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

class TaskPollingSchedulerTest {

    @Mock
    private ProducerTemplate producerTemplate;

    @InjectMocks
    private TaskPollingScheduler taskPollingScheduler;

    private AutoCloseable mocksCloser;

    @BeforeEach
    void setUp() {
        mocksCloser = openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocksCloser.close();
    }

    @Test
    void shouldPollSenaiteTasks() {
        taskPollingScheduler.pollTasks();

        verify(producerTemplate).sendBody(eq("direct:poll-senaite"), isNull());
    }
}
