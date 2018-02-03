#!/bin/bash
set -x
set -e

if [[ ! -f target/configuration-as-code.hpi ]]; then
    mvn install -o -DskipTests
fi
docker build -t jenkins/jenkins:lts-casc .

docker build -t demo milestone-1/

mkdir -p secrets
echo -n "123" > secrets/ADMIN_PASSWORD

# We mimic docker (swarm) secrets
docker run --rm -t -p 8080:8080 -v $(pwd)/secrets:/run/secrets:ro -v /var/run/docker.sock:/var/run/docker.sock demo
