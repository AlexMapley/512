#!/bin/bash
# Export classfile
export CLASSPATH=`pwd`:`pwd`/../servercode/ResInterface.jar
echo $CLASSPATH


# Compiling client
javac client.java




# Expect variable
keyword="Flight"

# Create stdin pipe
rm inPipe
mkfifo inPipe

# Spawns new client with pipe
# tail -f inPipe | java -Djava.security.policy=java.policy client $1 > outPipe &
tail -f inPipe | java -Djava.security.policy=java.policy client $1 > outLog &
processId=$!

# Feeds client-pipe commands in timed loop
for i in `seq 1 10`;
  do
  echo $i
  value="newflight,$i,2,3,4"
  echo $value
  echo $value > inPipe
  while true
  do
    cat outLog
    echo `tail -1 outLog`
    if [[ `tail -1 outLog | awk '{print $1;}'` == ">Transaction" ]]; then
      cat outLog
      echo > outLog
      break
    fi
  done
  sleep 1
done

# Kill client process
kill -9 $processId
