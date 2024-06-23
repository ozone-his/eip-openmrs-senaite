package com.ozonehis.eip.openmrs.senaite.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Item {
    @JsonProperty("uid")
    private String uid;

    @JsonProperty("path")
    private String path;

    @JsonProperty("AnalysisProfile")
    private AnalysisProfile analysisProfile;

    @JsonProperty("Analyses")
    private Analyses analyses;

    @JsonProperty("SampleType")
    private SampleType sampleType;
}

/*
{
    "count": 1,
    "pagesize": 25,
    "items": [
        {
            "uid": "1840746309b04725a927d1e501d8ff9b",
            "getCategoryTitle": null,
            "SamplePoint": null,
            "creation_date": "2022-10-18T07:42:32+00:00",
            "expirationDate": null,
            "rights": null,
            "AnalysisProfile": {
                "url": "http://localhost:8081/bika_setup/bika_analysisprofiles/analysisprofile-2",
                "uid": "f30a2a56d9194ea1a0f2044d5c0dc970",
                "api_url": "http://localhost:8081/senaite/@@API/senaite/v1/analysisprofile/f30a2a56d9194ea1a0f2044d5c0dc970"
            },
            "id": "artemplate-1",
            "subject": null,
            "state_title": null,
            "api_url": "http://localhost:8081/senaite/@@API/senaite/v1/artemplate/1840746309b04725a927d1e501d8ff9b",
            "modification_date": "2024-06-23T06:17:19+00:00",
            "title": "Renal function panel Template",
            "Composite": null,
            "Analyses": [
                {
                    "service_uid": "6f120fde274c411aa1946d7bda35e292",
                    "partition": "part-1"
                },
                {
                    "service_uid": "5085a3c31b6449c2b876be05cfe1e0fa",
                    "partition": "part-1"
                },
                {
                    "service_uid": "8c057c866cf4439ca2c78e2bfc89f6a0",
                    "partition": "part-1"
                },
                {
                    "service_uid": "8f379f7e017a4882b17d214e10c71b7a",
                    "partition": "part-1"
                },
                {
                    "service_uid": "8c7f6abfac324af2a44b5ab13853011e",
                    "partition": "part-1"
                }
            ],
            "effectiveDate": null,
            "parent_id": "bika_artemplates",
            "location": null,
            "contributors": null,
            "parent_url": "http://localhost:8081/senaite/@@API/senaite/v1/artemplates/4f042b8b606544c3b8eae02c2a14c1af",
            "review_state": "active",
            "Partitions": [
                {
                    "value": "",
                    "part_id": "part-1"
                }
            ],
            "SamplePointUID": null,
            "description": "161488AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
            "tags": [],
            "language": "en",
            "portal_type": "ARTemplate",
            "SampleType": {
                "url": "http://localhost:8081/bika_setup/bika_sampletypes/sampletype-1",
                "uid": "0c5a92ea879a4dafa13f60762d93e52e",
                "api_url": "http://localhost:8081/senaite/@@API/senaite/v1/sampletype/0c5a92ea879a4dafa13f60762d93e52e"
            },
            "Remarks": null,
            "allowedRolesAndUsers": [
                "Authenticated"
            ],
            "getKeyword": null,
            "path": "/senaite/bika_setup/bika_artemplates/artemplate-1",
            "AnalysisServicesSettings": [
                {
                    "hidden": false,
                    "uid": "6f120fde274c411aa1946d7bda35e292"
                },
                {
                    "hidden": false,
                    "uid": "5085a3c31b6449c2b876be05cfe1e0fa"
                },
                {
                    "hidden": false,
                    "uid": "8c057c866cf4439ca2c78e2bfc89f6a0"
                },
                {
                    "hidden": false,
                    "uid": "8f379f7e017a4882b17d214e10c71b7a"
                },
                {
                    "hidden": false,
                    "uid": "8c7f6abfac324af2a44b5ab13853011e"
                }
            ],
            "AutoPartition": true,
            "parent_uid": "4f042b8b606544c3b8eae02c2a14c1af",
            "SamplingRequired": null,
            "getClientUID": "",
            "getCategoryUID": null,
            "parent_path": "/senaite/bika_setup/bika_artemplates",
            "effective": "1000-01-01T00:00:00+00:00",
            "created": "2022-10-18T07:42:32+00:00",
            "url": "http://localhost:8081/bika_setup/bika_artemplates/artemplate-1",
            "author": "admin",
            "modified": "2024-06-23T06:17:19+00:00",
            "sortable_title": "renal function panel template",
            "allowDiscussion": null,
            "creators": [
                "admin"
            ]
        }
    ],
    "page": 1,
    "_runtime": 0.010547876358032227,
    "next": null,
    "pages": 1,
    "previous": null
}
 */
