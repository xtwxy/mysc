#! /bin/sh

TOTAL=0
i=0
while read -r LINE
do
    i=$((i+1))
    case "$LINE" in
        "") echo "blank line at line: $i ";;
        *" "*) echo "line with blanks at $i";;
        *[[:blank:]]*) echo "line with blanks at $i";;
        *) TOTAL=$((TOTAL+LINE)) ;;
    esac
done

echo $TOTAL
