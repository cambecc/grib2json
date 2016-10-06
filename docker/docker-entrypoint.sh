#! /bin/sh

set -ux
export PATH=$PATH:/usr/src/app/compiled/grib2json/bin

TEMP_UID="${TEMP_UID:-1000}"
useradd -s /bin/false --no-create-home -u ${TEMP_UID} temp
exec gosu temp $@