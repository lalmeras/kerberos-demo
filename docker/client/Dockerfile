FROM ubuntu

ENV DEBIAN_FRONTEND=noninteractive

# kerberos client, openjdk+groovy, psql client
RUN apt-get update && \
    apt-get install -y ntp krb5-config krb5-user dumb-init postgresql-client unzip openjdk-17-jre-headless wget && \
    wget -q https://dlcdn.apache.org/groovy/4.0.15/distribution/apache-groovy-binary-4.0.15.zip && \
    mkdir -p /opt && \
    unzip -d /opt apache-groovy-binary-4.0.15.zip && \
    ln -s /opt/groovy-4.0.15/bin/groovy /usr/bin/groovy && \
    rm -rf /var/lib/cache/apt

# kerberos client configuration
ENV KRB5_CONFIG=/etc/krb5.conf
COPY ./krb5-config/client/krb5.conf /etc/krb5.conf
COPY ./docker/client/files/jaas.conf /etc/jaas.conf
COPY ./docker/client/files/database.groovy /root/database.groovy
COPY ./docker/client/files/test.sh /root/test.sh

ENTRYPOINT ["/usr/bin/dumb-init", "--"]
CMD ["/usr/bin/sleep", "infinity"]