#!/bin/bash
# Export classfile
export CLASSPATH=`pwd`:`pwd`/../servercode/ResInterface.jar

# Usage
echo "arg1 is MiddleWare lab station"
echo "arg2 is unique client name"
echo "arg3 is either flight/car/room, for the target RM"

# Compiling client
javac client.java


# Create stdin pipe
mkfifo inPipe$$

# Spawns new client with pipe
# tail -f inPipe | java -Djava.security.policy=java.policy client $1 > outPipe &
tail -f inPipe$$ | java -Djava.security.policy=java.policy client $1 $2 > outLog$$ &
processId=$!

# Feeds client-pipe commands in timed loop
for i in `seq 1 30`;

  # 5.B block
  do
  echo "newflight,1,$i,1,1" > inPipe$$
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
  # 5.B block
  echo "newcar,1,mtl,1,1" > inPipe$$
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
  # 5.B block
  echo "newroom,1,mtl,1,1" > inPipe$$
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
