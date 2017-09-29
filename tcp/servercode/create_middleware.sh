# Exporting Class Path
export CLASSPATH="`pwd`"
echo $CLASSPATH

# Compiling codebase
javac ResInterface/ResourceManager.java
jar cvf ResInterface.jar ResInterface/*.class
javac ResImpl/MiddleWareImpl.java

# Setting permissions
chmod 704 ResInterface.jar
chmod 704 ResInterface/*.class
chmod 705 * # directory needs to be executable???

# Run server on registered instance
java -Djava.security.policy=java.policy -Djava.rmi.server.codebase="file:`pwd`" ResImpl.MiddleWareImpl
