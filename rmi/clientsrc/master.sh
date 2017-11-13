target="localhost"
target=$1
n=3
n=$2
dir=`pwd`

for i in `seq 1 $n`;
do
  ssh `whoami`@lab2-$i.cs.mcgill.ca "pwd; cd `pwd`; pwd; bash loop_client.sh $target flight;" &
done
