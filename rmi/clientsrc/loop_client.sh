#!/bin/bash
# Export classfile
export CLASSPATH=`pwd`:`pwd`/ResInterface.jar
echo $CLASSPATH

# Compiling server codebase
javac ResInterface/ResourceManager.java
jar cvf ResInterface.jar ResInterface/*.class
javac ResImpl/ResourceManagerImpl.java

# Compiling client
javac client.java


# Create stdin pipe
mkfifo in

# Spawns new client with pipe
tail -f in | java -Djava.security.policy=java.policy client $1



# Feeds client-pipe commands in timed loop
for i in `seq 1 2`;
  do
  value="newflight,"$i",2,3,4"
  echo $value > in
  sleep 0.2
done
