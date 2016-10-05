#! /bin/sh

set -ux
export PATH=$PATH:/usr/src/app/bin

TEMP_UID="${TEMP_UID:-1000}"
useradd -s /bin/false --no-create-home -u ${TEMP_UID} temp
exec gosu temp $@