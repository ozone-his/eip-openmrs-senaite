#!/usr/bin/env bash
#
# Copyright © 2021, Ozone HIS <info@ozone-his.com>
#
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#

STEP_CNT=6

echo_step() {
cat <<EOF
######################################################################
Init Step ${1}/${STEP_CNT} [${2}] -- ${3}
######################################################################
EOF
}
# Initialize the database
echo_step "1" "Starting" "Applying DB migrations"
superset db upgrade
echo_step "1" "Complete" "Applying DB migrations"

# Create an admin user
echo_step "2" "Starting" "Setting up admin user ( admin / $ADMIN_PASSWORD )"
superset fab create-admin \
              --username admin \
              --firstname Superset \
              --lastname Admin \
              --email admin@superset.com \
              --password $ADMIN_PASSWORD
echo_step "2" "Complete" "Setting up admin user"
# Create default roles and permissions
echo_step "3" "Starting" "Setting up roles and perms"
superset init

echo_step "3" "Complete" "Setting up roles and perms"
if [ "$SUPERSET_LOAD_EXAMPLES" = "yes" ]; then
    # Load some data to play with" row_number() over(partition by visit.patient_id order by visit.visit_id) as number_occurences," +
    echo_step "4" "Starting" "Loading examples"
    # If Cypress run which consumes superset_test_config – load required data for tests
    if [ "$CYPRESS_CONFIG" == "true" ]; then
        superset load_test_users
        superset load_examples --load-test-data
    else
        superset load_examples
    fi
    echo_step "4" "Complete" "Loading examples"
fi
echo_step "5" "Start" "Updating dashboards"
superset import-directory /dashboards -f -o
echo_step "5" "Complete" "Updating dashboards"
echo_step "6" "Start" "Updating datasources"
superset set_database_uri --database_name $ANALYTICS_DATASOURCE_NAME --uri postgresql://$ANALYTICS_DB_USER:$ANALYTICS_DB_PASSWORD@$ANALYTICS_DB_HOST:5432/$ANALYTICS_DB_NAME
echo_step "6" "Complete" "Updating datasources"