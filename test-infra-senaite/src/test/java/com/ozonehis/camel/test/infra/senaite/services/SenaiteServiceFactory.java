/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.camel.test.infra.senaite.services;

import lombok.NoArgsConstructor;
import org.apache.camel.test.infra.common.services.SimpleTestServiceBuilder;
import org.apache.camel.test.infra.common.services.SingletonService;

@NoArgsConstructor
public class SenaiteServiceFactory {

    static class SingletonSenaiteService extends SingletonService<SenaiteService> implements SenaiteService {

        public SingletonSenaiteService(SenaiteService service, String name) {
            super(service, name);
        }

        @Override
        public int getPort() {
            return getService().getPort();
        }

        public String getHost() {
            return getService().getHost();
        }

        @Override
        public String getHttpHostAddress() {
            return getService().getHttpHostAddress();
        }
    }

    public static SimpleTestServiceBuilder<SenaiteService> builder() {
        return new SimpleTestServiceBuilder<>("senaite");
    }

    public static SenaiteService createService() {
        return builder().addLocalMapping(SenaiteLocalContainerService::new).build();
    }

    public static SenaiteService createSingletonService() {
        return SingletonServiceHolder.INSTANCE;
    }

    private static class SingletonServiceHolder {

        static final SenaiteService INSTANCE;

        static {
            SimpleTestServiceBuilder<SenaiteService> instance = builder();
            instance.addLocalMapping(() -> new SingletonSenaiteService(new SenaiteLocalContainerService(), "senaite"));
            INSTANCE = instance.build();
        }
    }
}
