#!/bin/bash
#
# Copyright © 2021, Ozone HIS <info@ozone-his.com>
#
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#


set -eu

function create_user() {
	local user=$1
	local password=$2
	echo "Creating '$user' user..."
	psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" $POSTGRES_DB <<-EOSQL
	    CREATE USER $user WITH  PASSWORD '$password';
	    ALTER USER $user CREATEDB;
EOSQL
}

create_user ${ODOO_DB_USER} ${ODOO_DB_PASSWORD}
