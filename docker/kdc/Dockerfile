FROM ubuntu

ENV DEBIAN_FRONTEND=noninteractive

# kerberos server
RUN apt-get update && \
    apt-get install -y ntp krb5-admin-server krb5-kdc supervisor && \
    rm -rf /var/lib/cache/apt
# remove default configuration
RUN rm -r /var/lib/krb5kdc/
RUN rm -r /etc/krb5kdc/

# kerberos server configuration
ENV KRB5_CONFIG=/etc/krb5.conf
ENV KRB5_KDC_PROFILE=/var/kerberos/krb5kdc/kdc.conf
RUN mkdir -pv /var/kerberos/krb5kdc
COPY ./krb5-config/server/kdc.conf /var/kerberos/krb5kdc/kdc.conf
COPY ./krb5-config/server/kadm5.acl /var/kerberos/krb5kdc/kadm5.acl
COPY ./krb5-config/client/krb5.conf /etc/krb5.conf
RUN mkdir -pv /var/log/kerberos/
RUN touch /var/log/kerberos/krb5.log
RUN touch /var/log/kerberos/kadmin.log
RUN touch /var/log/kerberos/krb5lib.log
RUN kdb5_util -r EXAMPLE.COM -P krb5 create -s

# supervisord
COPY ./docker/kdc/files/supervisord.conf /etc/supervisord.conf
COPY ./docker/kdc/files/init.sh /root/init.sh

# when container is starting
CMD ["/usr/bin/bash", "-c", "/root/init.sh && exec /usr/bin/supervisord -n -c /etc/supervisord.conf"]