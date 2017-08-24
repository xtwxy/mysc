#!/bin/sh

find . -type f -name "*.jar" -exec ls -l {} \; | awk '{ print $5 }' | sh sumline.sh

