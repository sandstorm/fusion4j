#!/bin/bash

find ../neos-development-collection -name '*.fusion' -print0 |
  while IFS= read -r -d '' line; do
    targetFile="default-fusion/src/main/fusion$(echo "$line" | sed 's/\.\.\/neos-development-collection//g' | sed 's/Neos\./Neos__/g')"
    mkdir -p "$(dirname ${targetFile})"
    cp "$line" "$targetFile"
  done
