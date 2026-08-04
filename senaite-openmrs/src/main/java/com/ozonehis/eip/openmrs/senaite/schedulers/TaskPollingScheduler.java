/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.schedulers;

import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TaskPollingScheduler {

    @Autowired
    private ProducerTemplate producerTemplate;

    @Scheduled(initialDelayString = "${task.update.initial.delay}", fixedDelayString = "${task.update.delay}")
    public void pollTasks() {
        producerTemplate.sendBody("direct:poll-senaite", null);
    }
}
