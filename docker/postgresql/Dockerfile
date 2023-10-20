FROM postgres:16

ENV DEBIAN_FRONTEND=noninteractive

# kerberos client
RUN apt-get update && \
    apt-get install -y ntp krb5-config krb5-user && \
    rm -rf /var/lib/cache/apt

# kerberos client configuration
ENV KRB5_CONFIG=/etc/krb5.conf
COPY ./krb5-config/client/krb5.conf /etc/krb5.conf