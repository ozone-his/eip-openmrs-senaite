/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.config;

import org.openmrs.eip.app.config.AppConfig;
import org.openmrs.eip.fhir.spring.OpenmrsFhirAppConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Import the {@link AppConfig} class to ensure that the required beans are created.
 */
@Configuration
@Import({AppConfig.class, OpenmrsFhirAppConfig.class})
public class EIPAppConfig {}
