#! /bin/bash

set -e

# Conditionnaly create users
if [[ ! -f "/etc/postgres-service.keytab" ]]; then
    cat << EOF  | kadmin.local
add_principal -randkey "postgres/postgresql.example.com@EXAMPLE.COM"
ktadd -k /etc/postgresql-service.keytab -norandkey "postgres/postgresql.example.com@EXAMPLE.COM"
listprincs
quit
EOF
fi

if [[ ! -f "/etc/kuser.keytab" ]]; then
    cat << EOF  | kadmin.local
add_principal -randkey "kuser@EXAMPLE.COM"
ktadd -k /etc/kuser.keytab -norandkey "kuser@EXAMPLE.COM"
listprincs
quit
EOF
fi

# Update keytabs
mkdir -p /etc/keytabs/postgresql
mkdir -p /etc/keytabs/client

cp /etc/postgresql-service.keytab /etc/keytabs/postgresql
cp /etc/kuser.keytab /etc/keytabs/client

chmod ugo+rwx /etc/keytabs/postgresql/*
chmod ugo+rwx /etc/keytabs/client/*

find /etc/keytabs -ls