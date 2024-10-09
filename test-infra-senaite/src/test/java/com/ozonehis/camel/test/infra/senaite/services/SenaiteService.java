/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.camel.test.infra.senaite.services;

import org.apache.camel.test.infra.common.services.TestService;

public interface SenaiteService extends TestService {

    int getPort();

    String getHost();

    default String getHttpHostAddress() {
        return String.format("%s:%d", getHost(), getPort());
    }
}
