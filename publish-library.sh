#!/usr/bin/env bash

rm -rf _artifacts

docker compose build && docker compose run --rm -v "$(pwd)":/src -w /src -u "$(id -u)":"$(id -g)" gradle \
  :querydroid:testReleaseUnitTest \
  :querydroid:publishToSonatype \
  :closeAndReleaseSonatypeStagingRepository
EXIT_CODE=${PIPESTATUS[0]}

cp -r querydroid/build _artifacts

exit "${EXIT_CODE}"
