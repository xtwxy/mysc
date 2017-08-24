#! /bin/sh

total=0

while read -r line
do
    if [ "$line" != "" ]; then
        total=$((total + line))
    fi
done

echo $total

