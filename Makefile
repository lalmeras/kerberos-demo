.PHONY: clean containers containers-daemons containers-stop test

default: clean containers-daemons test

# clean images and containers
clean:
	docker-compose rm -fsv
	-docker image rm krb5-client krb5-kdc krb5-postgresql

# start containers (foreground)
containers:
	docker-compose up

# start containers (background)
containers-daemons:
	docker-compose up -d

# stop containers
containers-stop:
	docker-compose stop

# launch client psql and java test
test:
	sleep 10
	docker exec -ti krb5-client /root/test.sh