#!/bin/bash
# Export classfile
export CLASSPATH=`pwd`:`pwd`/../servercode/ResInterface.jar

# Usage
echo "arg1 is MiddleWare lab station"
echo "arg2 is either flight/car/room, for the target RM"


# Compiling client
javac client.java

# RM to target
keyword="newflight"  #default
keyword="new"$2

# Create stdin pipe
mkfifo inPipe$$

# Spawns new client with pipe
# tail -f inPipe | java -Djava.security.policy=java.policy client $1 > outPipe &
tail -f inPipe$$ | java -Djava.security.policy=java.policy client $1 > outLog$$ &
processId=$!

# Feeds client-pipe commands in timed loop
for i in `seq 1 50`;
  do
  if [[ $keyword == "newflight" ]]; then
    value="$keyword,1,$i,3,4"
  else
    value="$keyword,1,mtl,3,4"
  fi
  echo $value
  echo $value > inPipe$$
  break=0
  while [[ $break -eq 0 ]]
  do
    sleep 0.5s
    if [[ `tail -1 outLog$$` = ">" ]]; then
      sleep 0.5s
      tail -9 outLog$$
      break=1
      break
    fi
  done

done

# Kill client process
kill -9 $processId
