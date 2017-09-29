# Export classfile
# From own machine!!!
#cp servercode/ResInterface.jar clientsrc/ResInterface.jar

export CLASSPATH=`pwd`:`pwd`/ResInterface.jar
echo $CLASSPATH

# Compile client
jar cvf ResInterface.jar ../servercode/ResInterface/*.class
javac -cp ../servercode/ResInterface/* client.java

# Run Client ON LOCAL INSTANCE
java -Djava.security.policy=java.policy client cs-3 # Connects to lab1-2
