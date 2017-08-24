#! /bin/sh

i=0
while read -r line
do
    i=$((i+1))
    case "$line" in
        "") echo "blank line at line: $i ";;
        *" "*) echo "line with blanks at $i";;
        *[[:blank:]]*) echo "line with blanks at $i";;
    esac
done
