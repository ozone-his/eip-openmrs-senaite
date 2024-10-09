#!/usr/bin/env bash
#
# Copyright © 2021, Ozone HIS <info@ozone-his.com>
#
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#


# Apply all scripts found in docker-entrypoint-initdb.d/db/ directory.
#
# Workaround for PostgreSQL Docker image not running files from sub-dirs.
# https://github.com/docker-library/postgres/issues/605#issuecomment-567236795
#
#
set -e
directory=/docker-entrypoint-initdb.d/db/
if [ -d "${directory}" ]
then
    find ${directory}* -type f -execdir ls {} \; -execdir bash {} \;
else
    echo "Directory '${directory}' does not exist. No database to create. Exit 0."
fi
exit 0