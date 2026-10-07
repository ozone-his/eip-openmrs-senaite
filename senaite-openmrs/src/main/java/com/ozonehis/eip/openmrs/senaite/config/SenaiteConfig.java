/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Component
public class SenaiteConfig {
    @Value("${senaite.username}")
    private String senaiteUsername;

    @Value("${senaite.password}")
    private String senaitePassword;

    @Value("${senaite.baseUrl}")
    private String senaiteBaseUrl;

    public String authHeader() {
        if (StringUtils.isEmpty(getSenaiteUsername())) {
            throw new IllegalArgumentException(
                    "Cannot authenticate with SENAITE: configure a non-empty 'senaite.username' property");
        }
        if (StringUtils.isEmpty(getSenaitePassword())) {
            throw new IllegalArgumentException(
                    "Cannot authenticate with SENAITE: configure a non-empty 'senaite.password' property");
        }
        String auth = getSenaiteUsername() + ":" + getSenaitePassword();
        byte[] encodedAuth = Base64.encodeBase64(auth.getBytes());
        return "Basic " + new String(encodedAuth);
    }
}
