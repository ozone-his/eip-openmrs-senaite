/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.camel.test.infra.senaite.services;

import com.ozonehis.camel.test.infra.senaite.common.SenaiteProperties;
import java.io.File;
import java.net.URL;
import java.time.Duration;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

@Slf4j
public class SenaiteLocalContainerService implements SenaiteService {

    private final ComposeContainer container;

    private final String SERVICE_NAME = "senaite";

    public SenaiteLocalContainerService() {
        this.container = initContainer();
    }

    @Override
    public int getPort() {
        return container.getServicePort(SERVICE_NAME, SenaiteProperties.DEFAULT_SERVICE_PORT);
    }

    @Override
    public String getHost() {
        return container.getServiceHost(SERVICE_NAME, SenaiteProperties.DEFAULT_SERVICE_PORT);
    }

    @Override
    public void registerProperties() {
        System.setProperty(SenaiteProperties.SENAITE_HOST, getHost());
        System.setProperty(SenaiteProperties.SENAITE_PORT, String.valueOf(getPort()));
    }

    @Override
    public void initialize() {
        log.info("Starting SENAITE container...");
        container.start();

        registerProperties();
        log.info("SENAITE container started");
    }

    @Override
    public void shutdown() {
        log.info("Stopping the SENAITE container.");
        container.stop();
        log.info("SENAITE container stopped.");
    }

    protected ComposeContainer initContainer() {
        try (var container = new ComposeContainer(getFile("docker-compose/docker-compose-senaite.yml"))
                .withLocalCompose(true)
                .withStartupTimeout(java.time.Duration.ofMinutes(5))
                .withExposedService(
                        SERVICE_NAME,
                        SenaiteProperties.DEFAULT_SERVICE_PORT,
                        Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(5)))) {

            return container;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected File getFile(String fileName) {
        URL url = getClass().getClassLoader().getResource(fileName);
        return new File(Objects.requireNonNull(url).getPath());
    }
}
