#! /usr/bin/bash

set -e

# test psql connection
test_psql() {
    kinit -k -t /etc/keytabs/client/kuser.keytab kuser
    psql -h postgresql.example.com -U kuser kuser -c "SELECT 1"
}

test_java() {
    # test java connection
    groovy /root/database.groovy -u kuser -g require -j /etc/jaas.conf -k /etc/keytabs/client/kuser.keytab jdbc:postgresql://postgresql.example.com/kuser
    echo "[SUCCESS] java connection"
}

test_psql 2>&1 >/dev/null && echo "[SUCCESS] psql connection" || echo "[FAILED] psql connection"
test_java 2>&1 >/dev/null && echo "[SUCCESS] java connection" || echo "[FAILED] java connection"