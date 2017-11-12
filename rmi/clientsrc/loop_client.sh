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
  value="newflight,$i,$i,3,4"
  echo $value > inPipe
  while true
  do
    sleep 0.1s
    echo `tail -1 outLog`
    if [[ `tail -1 outLog` = ">" ]]; then
      tail -9 outLog
      break
    fi
  done
  sleep 0.5s
done

# Cleanup
# Kill client process
kill -9 $processId
rm outLog
rm .nfs*
