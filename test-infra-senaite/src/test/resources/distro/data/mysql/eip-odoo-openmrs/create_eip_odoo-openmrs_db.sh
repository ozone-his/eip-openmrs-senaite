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
mysql --password=$MYSQL_ROOT_PASSWORD --user=root <<MYSQL_SCRIPT
    CREATE DATABASE $1;
    CREATE USER '$2'@'localhost' IDENTIFIED BY '$3';
    CREATE USER '$2'@'%' IDENTIFIED BY '$3';
    GRANT ALL PRIVILEGES ON $1.* TO '$2'@'localhost';
    GRANT ALL PRIVILEGES ON $1.* TO '$2'@'%';
    FLUSH PRIVILEGES;
MYSQL_SCRIPT
}

create_eip_client_user_and_database() {
	local dbName="${1:-}"
	local dbUser="${2:-}"
	local dbUserPassword="${3:-}"
	if [ "${dbName:-}" ] && [ "${dbUser:-}" ] && [ "${dbUserPassword:-}" ]; then
		create_user_and_database "$dbName" "$dbUser" "$dbUserPassword";
	fi
}

echo "Creating '${EIP_DB_USER_ODOO}' user and '${EIP_DB_NAME_ODOO}' database..."
create_eip_client_user_and_database ${EIP_DB_NAME_ODOO:-} ${EIP_DB_USER_ODOO:-} ${EIP_DB_PASSWORD_ODOO:-};
