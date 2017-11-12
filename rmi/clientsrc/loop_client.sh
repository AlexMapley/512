#!/bin/bash
# Export classfile
export CLASSPATH=`pwd`:`pwd`/../servercode/ResInterface.jar

# Usage
echo "arg1 is MiddleWare lab station"
echo "arg2 is either flight/car/room, for the target RM"


# Compiling client
javac client.java

# RM to target
keyword="flight"  #default
keyword=$2
kewyword="new"$keyword

# Create stdin pipe
rm inPipe
mkfifo inPipe

# Spawns new client with pipe
# tail -f inPipe | java -Djava.security.policy=java.policy client $1 > outPipe &
tail -f inPipe | java -Djava.security.policy=java.policy client $1 > outLog &
processId=$!

# Feeds client-pipe commands in timed loop
for i in `seq 1 50`;
  do
  if [[ $kewyord == "newflight "]]; then
    value="$keyword,$i,$i,3,4"
  else
    value="$keyword,mtl$i,3,4"
  fi
  echo $value > inPipe
  break=0
  while [[ $break -eq 0 ]]
  do
    sleep 0.5
    if [[ `tail -1 outLog` = ">" ]]; then
      sleep 0.5
      tail -9 outLog
      break=1
      break
    fi
  done

done

# Cleanup
# Kill client process
kill -9 $processId
rm outLog
rm inPipe
kill -9 $(lsof +D `pwd` | awk '!/bash/' | awk '!/lsof/' | awk '{print $2}')
