# Accepts lab, name, then year as args, in that order
if [[ $1 ]]; then
	LAB=$1
fi
if [[ $2 ]]; then
	NAME=$2
fi
if [[ $3 ]]; then
	YEAR=$3
fi


echo Lab: $LAB
echo NAME: $NAME
echo YEAR: $YEAR

# Export classfile
# From own machine!!!
cp servercode/ResInterface.jar clientsrc/ResInterface.jar
export CLASSPATH=`pwd`/clientsrc:`pwd`/servercode/ResInterface.jar
echo $CLASSPATH
echo

# Compile client
javac -cp  servercode/ResInterface/* clientsrc/client.java 

# Run Client ON LOCAL INSTANCE
java -Djava.security.policy=java.policy client 5959
