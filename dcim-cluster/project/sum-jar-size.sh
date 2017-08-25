#!/bin/sh
PRG_DIR=$(dirname $(readlink -f $0))
find . -type f -name "*.jar" -exec ls -l {} \; | awk '{ print $5 }' | sh $PRG_DIR/sumline.sh

