# Export classfile
# From own machine!!!
#cp servercode/ResInterface.jar clientsrc/ResInterface.jar

export CLASSPATH=`pwd`:`pwd`/ResInterface.jar
echo $CLASSPATH

# Compile client
jar cvf ResInterface.jar ResInterface/*.class
javac client.java

# Run Client ON LOCAL INSTANCE
java client
