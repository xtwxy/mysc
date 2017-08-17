#! /bin/sh

PRG_DIR=$(dirname $(readlink -f $0))
cd $PRG_DIR
java -DHOSTNAME=192.168.0.78 -DHOST=192.168.0.78 -DPORT=2553 -DHTTPPORT=8080 -jar target/scala-2.12/cluster.jar

