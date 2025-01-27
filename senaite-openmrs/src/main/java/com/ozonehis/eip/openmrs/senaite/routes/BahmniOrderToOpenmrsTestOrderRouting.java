/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openmrs.senaite.routes;

import com.ozonehis.eip.openmrs.senaite.converters.ResourceConverter;
import com.ozonehis.eip.openmrs.senaite.processors.TaskProcessor;
import lombok.Setter;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.sql.SqlComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BahmniOrderToOpenmrsTestOrderRoute extends RouteBuilder {
	
	@Value("${bahmni.test.orderType.uuid}")
	private String bahmniTestOrderTypeUuid;
    
    @Override
    public void configure() throws Exception {

    	// spotless:off
    	from("direct:write-bahmniOrder-as-openmrs-testOrder")
            .routeId("write-bahmniOrder-as-testOrder")
            .log(LoggingLevel.INFO, "Processing Test Order from Order ::::::  ${exchangeProperty.event.tableName}")
            .choice()
                .when(simple("${exchangeProperty.event.tableName} == 'orders' && ${exchangeProperty.event.operation} == 'c'"))
                    // Query database to check if the order already exists in test_order table
                    .toD("sql:SELECT COUNT(*) total FROM test_order to  WHERE order_id=${exchangeProperty.event.primaryKeyId}?dataSource=openmrsDataSource")
                    
                    .choice()
                    	.when(simple("${body[0]['total']} == 0"))  // No related test order found
                    	.toD("sql:SELECT ot.uuid as order_type_uuid from order_type ot join orders o on o.order_type_id = ot.order_type_id where o.uuid ='${exchangeProperty.event.identifier}'?dataSource=#openmrsDataSource")
                            .choice()
                                .when(simple("${body[0]['order_type_uuid']} == '${bahmniTestOrderTypeUuid}'"))  // Check if it's the correct order type
                                	.log(LoggingLevel.INFO, "aaaaaaaaaaaa Order type : ${bahmniTestOrderTypeUuid}")
                                	// Insert a new record into test_order if it's the correct order type
                                    .toD("sql:INSERT INTO test_order(order_id) VALUES (${exchangeProperty.lab-order-id})?dataSource=openmrsDataSource")
                            .endChoice()
                        .endChoice()

                    .otherwise()
                        // Do nothing if the related test order exists
                        .log(LoggingLevel.DEGUG, "Related test order exists, no action needed.")
                    .endChoice()
                .endChoice()
            .end();
        // spotless:on
    }
}