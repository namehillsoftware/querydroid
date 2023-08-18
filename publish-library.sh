#!/usr/bin/env bash

rm -rf _artifacts

docker compose build && docker compose run --rm -v "$(pwd)":/src -w /src -u "$(id -u)":"$(id -g)" gradle \
  :library:publishToSonatype \
  :library:closeAndReleaseSonatypeStagingRepository
EXIT_CODE=${PIPESTATUS[0]}

cp -r library/build _artifacts

exit "${EXIT_CODE}"
