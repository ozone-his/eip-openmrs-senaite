/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.routes.analysisrequest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class AnalysisRequestCatalogQuery {

    public static final int PAGE_SIZE = 50;

    private AnalysisRequestCatalogQuery() {}

    public static String publishedSince(String watermark, int offset) {
        return query("published", "getDatePublished", watermark, offset);
    }

    public static String receivedSince(String watermark, int offset) {
        return query("sample_received", "getDateReceived", watermark, offset);
    }

    static String query(String reviewState, String dateIndex, String watermark, int offset) {
        return "catalog=senaite_catalog_sample"
                + "&review_state=" + reviewState
                + "&sort_on=" + dateIndex
                + "&sort_order=ascending"
                + "&limit=" + PAGE_SIZE
                + "&b_start=" + offset
                + "&" + dateIndex + ".query:record=" + URLEncoder.encode(watermark, StandardCharsets.UTF_8)
                + "&" + dateIndex + ".range:record=min";
    }
}
