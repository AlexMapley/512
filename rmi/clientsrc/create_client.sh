#!/bin/bash
# Export classfile
export CLASSPATH=`pwd`:`pwd`/../servercode
echo $CLASSPATH

# Compile client
javac client.java

# Run client on either localhost [default] or a target server
java -Djava.security.policy=java.policy client $1 $2
