LANG := en_US.UTF-8
SHELL         := /usr/bin/env bash
DOCKER_USER    ?= sba-research.at/wire
DOCKER_TAG     ?= local
M2_SYSTEM      := /home/n0s/.m2
LIB_PINNING    := repository/at/sbaresearch/mqtt4android/pinning-tls-lib

.PHONY: install
install:
	./mvnw package

.PHONY: docker
docker:
	mkdir -p m2-docker/$(LIB_PINNING)
	cp -r $(M2_SYSTEM)/$(LIB_PINNING)/* m2-docker/$(LIB_PINNING)
	docker build -t $(DOCKER_USER)/push-integration:$(DOCKER_TAG) .
