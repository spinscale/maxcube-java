#!/usr/bin/env bash

docker history airhacks/micro
docker save -o max-web-docker-image dad12a31defd
scp max-web-docker-image pi:
ssh pi
curl -sSL get.docker.com | sh
sudo docker load -i max-web-docker-image


# in one command:
#
# docker save dad12a31defd | bzip2 | ssh pi 'bunzip2 | docker load'
