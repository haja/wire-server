#!/bin/bash

DOCKER_USER=$1
DOCKER_TAG=$2

if ! docker image ls | grep ${DOCKER_USER}/alpine-rebuildable > /dev/null; then
	echo "'make docker-rebuildable' required."
	exit 1
fi

if docker container ls -a | grep -F "update-rebuilder" > /dev/null; then
	echo "dangling container, removing"
	docker container rm update-rebuilder || exit 1;
fi

# create and run container in background
docker run -dt --name update-rebuilder ${DOCKER_USER}/alpine-rebuildable:${DOCKER_TAG} &&

#  copy all sources except `.stack-work` and useless stuff from `wire-server` (THIS REQUIRES DOCKER >= 1.8) and rebuild
tar -c --exclude=".stack-work" --exclude=".git" --exclude="dist" . | docker cp - update-rebuilder:wire-server
if docker exec update-rebuilder sh -c "make fast"; then
    OLD_IMAGE=$(docker images ${DOCKER_USER}/alpine-rebuildable:local --quiet)

    docker commit --message="rebuilded" update-rebuilder ${DOCKER_USER}/alpine-rebuildable:${DOCKER_TAG} &&
    docker tag ${DOCKER_USER}/alpine-rebuildable:${DOCKER_TAG} ${DOCKER_USER}/alpine-rebuildable:latest &&

    # remove container so next run can reuse the name
    docker stop update-rebuilder &&
    docker container rm update-rebuilder &&
    docker rmi ${OLD_IMAGE} || echo "WARN image ${OLD_IMAGE} could not be removed"

    # minify to be intermediate-compatible
    OLD_IMAGE=$(docker images ${DOCKER_USER}/alpine-intermediate:local --quiet)
    echo "(DEBUG) old image: $OLD_IMAGE"
    docker build -t ${DOCKER_USER}/alpine-intermediate:${DOCKER_TAG} -f build/alpine/Dockerfile.rebuilded-intermediate --build-arg deps=${DOCKER_USER}/alpine-deps . &&
    docker tag ${DOCKER_USER}/alpine-intermediate:${DOCKER_TAG} ${DOCKER_USER}/alpine-intermediate:latest &&

    docker rmi ${OLD_IMAGE} || echo "WARN image ${OLD_IMAGE} could not be removed"
else
    echo "rebuild failed"
    docker stop update-rebuilder
fi


