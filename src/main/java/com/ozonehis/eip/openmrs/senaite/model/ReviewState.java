/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.model;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ReviewState implements SenaiteResource {
    SAMPLE_DUE("sample_due"),
    SAMPLE_RECEIVED("sample_received"),
    PUBLISHED("published"),
    CANCELLED("cancelled");

    private final String value;

    @JsonValue
    public String getValue() {
        return value;
    }
}
