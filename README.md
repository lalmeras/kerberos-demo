# Kerberos demonstration

## Prerequisites

* Free TCP ports 1088, 1749, 6432

## One-liner

```
# Build, make and test
docker exec -ti krb5-client /root/test.sh
[...]
[SUCCESS] psql connection
[SUCCESS] java connection
```

## Manual checks

```
# Build and start kerberos stack
make clean containers
```

Once krb5-client, krb5-kdc and krb5-postgresql are up:

psql command and output (from krb5-client):

```
docker exec -ti krb5-client /usr/bin/bash
kinit -k -t /etc/keytabs/client/kuser.keytab kuser
psql -h postgresql.example.com -U kuser kuser

[...]
psql (14.9 (Ubuntu 14.9-0ubuntu0.22.04.1), server 16.0 (Debian 16.0-1.pgdg120+1))
[...]
GSSAPI-encrypted connection
[...]
```

java command and output (from krb5-client):

```
docker exec -ti krb5-client /usr/bin/bash
groovy /root/database.groovy -u kuser -g require -j /etc/jaas.conf -k /etc/keytabs/client/kuser.keytab jdbc:postgresql://postgresql.example.com/kuser

[...]
Success
```

## Global setup

* `kdc` provides a Kerberos identity server for `postgres/postgresql.example.com@EXAMPLE.COM` service
  and `kuser@EXAMPLE.COM` user.
* keytabs are generated for both credentials.
* `kuser@EXAMPLE.COM` keytab is available on client container.
* `postgres/postgresql.example.com@EXAMPLE.COM` keytab is available on postgresql container.
* `kdc` can be reached by ports 88 and 749 (exposed to outside through 1088 and 1749).
* `postgresql` can be reached by port 5432 (exposed to outside through 6432).
* `kuser@EXAMPLE.COM` is mapped to postgresql `kuser`

## Connection from localhost

TCP 1088 and 1749 ports are exported for kdc server.

TCP 6432 port is exported for database access.

Check Kerberos `/etc/krb5.conf`:

```
[...]
[libdefaults]
dns_canonicalize_hostname = fallback

[...]
[realms]
[...]
EXAMPLE.COM = {
    kdc = kdc.example.com:1088
    admin_server = kdc.example.com:1749
    dns_lookup_realm = false
    permitted_enctypes = aes256-cts-hmac-sha384-192
    default_tkt_enctypes = aes256-cts-hmac-sha384-192
    default_tgs_enctypes = aes256-cts-hmac-sha384-192
}

[...]
[domain_realm]
[...]
.example.com = EXAMPLE.COM
example.com = EXAMPLE.COM

```

Check `/etc/hosts` configuration:

```
127.0.0.1 kdc.example.com postgresql.example.com
```

Perform psql connection:

```
# Keytab lookup
docker exec krb5-client cat /etc/keytabs/client/kuser.keytab > /tmp/kuser.keytab
# Keytab loading
kinit -k -t /tmp/kuser.keytab kuser@EXAMPLE.COM
# psql connection
psql -U kuser -h postgresql.example.com -p 6432
```

Perform java connection (check paths): (groovy command needed)

```
docker exec krb5-client cat /etc/keytabs/client/kuser.keytab > /tmp/kuser.keytab
groovy ./docker/client/files/database.groovy -u kuser -g require -j ./docker/client/files/jaas.conf -k /tmp/kuser.keytab jdbc:postgresql://postgresql.example.com:6432/kuser
```

## Kerberos documentation

### docker-compose

Network setup and aliases are needed:

* hostname used by psql client `postgresql.example.com` is used to retrieve the
  server-side service principal `postgres/postgresql.example.com@EXAMPLE.COM`
* `kdc.example.com` is used in kerberos client configuration

### krb5-kdc

This container runs Kerberos Key Distribution Center. Kerberos is installed with
.deb packages. A common `krb5.conf` configuration is shared between server
and clients. It setups an `EXAMPLE.com` realm.

When container is created, `docker/kdc/files/init.sh` creates
`postgres/postgresql.example.com@EXAMPLE.COM` (service) and `kuser@EXAMPLE.COM`
(user) credentials. Both credentials are key-based. It exports credentials with
`ktadd -norandkey` to keytab files.

`-norandkey` prevents ktadd to update the current principal key.

`/etc/keytabs/client/kuser.keytab` and `/etc/keytabs/postgresql/postgresql-service.keytab`
are copied to shared docker volumes `postgresql` and `client`, so that they can be
reused on client and postgresql containers.

`dns_canonicalize_hostname = fallback` prevents Kerberos to canonicalize hostname by
performance hostname > IP > reverse DNS hostname resolution. If a principal with
hostname is found, canonicalize is not performed.

`*_enctypes` are important as kerberos server and clients must be configured with
accordingly (`krb5.conf` and `kdc.conf`).

`domain_realm` from `krb5.conf` is important so that realm is correctly determined from
hostnames.

### krb5-postgresql

This container runs a standard postgres docker, with added kerberos configuration:

* Kerberos client configuration: `/etc/krb5.conf`
* Postgresql kerberos configuration for service principal `postgres/postgresql.example.com@EXAMPLE.COM`:
  `krb_server_keyfile = '/etc/keytabs/postgresql/postgresql-service.keytab'` in `postgresql.conf`
  (file is `postgresql` shared docker volume)
* Postgresql `pg_hba.conf` and `pg_init.conf` to map kerberos authentication to
  postgresql user
* Note: these configurations is done when container is created.

### krb5-client

This container allows to run a kerberos-backed psql connection. `/etc/keytabs/client/kuser.keytab`
keytab is used to perform connection.

## Troubleshooting

### Canonicalization

Wrong or unset `dns_canonicalize_hostname` setup triggers the following error:

```
could not initiate GSSAPI security context: Unspecified GSS failure.  Minor code may provide more information: Server krbtgt/KERBEROS-DEMO_DEFAULT@EXAMPLE.COM not found in Kerberos database
```

### pg_hba.conf

```
psql: error: connection to server at "postgresql.example.com" (172.23.0.4), port 5432 failed: FATAL:  no pg_hba.conf entry for host "172.23.0.3", user "kuser", database "kuser", GSS encryption
```

Check that `/var/lib/postgresql/data/pg_hba.conf` in postgresql container is correct.

### Kerberos client traces

Add `KRB5_TRACE=/dev/stderr` as environment to display kerberos trace logs. It allows to
check that principal are correct and the step where error is triggered.

Example:

```
KRB5_TRACE=/dev/stderr psql -U kuser -h postgresql.example.com -p 6432
```

Add `-d` option to database.groovy script to enable verbose logging:

```
groovy /root/database.groovy -u kuser -g require -j /etc/jaas.conf jdbc:postgresql://postgresql.example.com/kuser
```

## File list

* README.md: this file
* docker-compose.yml:
  * client, kdc and postgresql services. kdc and postgresql are exposed with ports
    tcp/1088, tcp/1749, tcp/6432
  * postgresql and client volumes allow to share keytab with kdc container
* docker/*/Dockerfile:
  * client: install kerberos psql and java/groovy clients
  * kdc: Kerberos server
  * postgresql: postgresql container + kerberos configuration installed during database init
* docker/*/files:
  * client: jaas.conf and groovy script to test java connection
  * kdc: init.sh to create principals and export to keytabs; supervisord.conf to run kerberos
    services
  * postgresql: init.sh to install pg_hba.conf, pg_init.conf and to modify postgresql.conf
* krb5-config/client: common configuration for clients
* krb5-config/server: server only configuration

## Groovy script

|
```
Usage: database [-dhV] [-c=<krb5Conf>] [-g=<gssEncMode>] [-j=<jaasConf>]
                [-k=<keytab>] [-p=<password>] -u=<username> JDBC_URL
Test database connection.
      JDBC_URL            JDBC URL for connection.
  -c, --krb5-conf=<krb5Conf>
                          krb5.conf file
  -d, --debug             GSS debug messages
  -g, --gss-enc-mode=<gssEncMode>
                          GssEncMode setting
  -h, --help              Show this help message and exit.
  -j, --jaas-conf=<jaasConf>
                          jaas.conf file
  -k, --keytab=<keytab>   keytab file
  -p, --password=<password>
                          Database connection password
  -u, --user=<username>   Database connection username
  -V, --version           Print version information and exit.
```
