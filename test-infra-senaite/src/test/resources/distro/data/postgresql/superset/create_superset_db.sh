#
# Copyright © 2021, Ozone HIS <info@ozone-his.com>
#
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#

export db_name=$SUPERSET_DB
export db_username=$SUPERSET_DB_USER
export db_password=$SUPERSET_DB_PASSWORD


echo "Creating '$db_username' user and '$db_name' database..."

createuser ${db_username}
createdb ${db_name}

psql -d ${db_name} -c "alter user ${db_username} with password '${db_password}';"
psql -d ${db_name} -c "grant all privileges on database ${db_name} to ${db_username};"
