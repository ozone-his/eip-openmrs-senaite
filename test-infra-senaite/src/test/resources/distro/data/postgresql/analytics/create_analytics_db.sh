#!/bin/bash
#
# Copyright © 2021, Ozone HIS <info@ozone-his.com>
#
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#


set -eu

function create_user_and_database() {
	local database=$1
	local user=$2
	local password=$3
	echo "Creating '$user' user and '$database' database..."
	psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" $POSTGRES_DB <<-EOSQL
	    CREATE USER $user WITH  PASSWORD '$password';
	    CREATE DATABASE $database;
	    GRANT ALL PRIVILEGES ON DATABASE $database TO $user;
EOSQL
}

create_user_and_database ${ANALYTICS_DB_NAME} ${ANALYTICS_DB_USER} ${ANALYTICS_DB_PASSWORD}
