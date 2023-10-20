#! /usr/bin/bash

set -e

# Setup access Kerberos
cp /var/lib/postgres/pg_hba.conf /var/lib/postgres/pg_ident.conf /var/lib/postgresql/data/

echo -e "\nkrb_server_keyfile = '/etc/keytabs/postgresql/postgresql-service.keytab'" >> /var/lib/postgresql/data/postgresql.conf