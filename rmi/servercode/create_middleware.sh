# Register RMI
rmiregistry -J-Djava.rmi.server.useCodebaseOnly=false 5959 &

# Exporting Class Path
export CLASSPATH="`pwd`"
echo $CLASSPATH

# Compiling codebase
javac ResInterface/ResourceManager.java
jar cvf ResInterface.jar ResInterface/*.class
javac ResImpl/MiddleWareImpl.java
javac TransImpl/CrashDetection.java

# Copying jar file to client source
cp ResInterface.jar ../clientsrc/ResInterface.jar
jar cvf TransImpl.jar TransImpl/*.class
chmod 704 TransImpl.jar
#mv TransImpl ../clientsrc/TransImpl

# Setting permissions
chmod 704 ResInterface.jar
chmod 704 ResInterface/*.class
chmod 705 * # directory needs to be executable???

# Run server on registered instance
java -Djava.security.policy=java.policy -Djava.rmi.server.codebase="file:`pwd`" ResImpl.MiddleWareImpl $1 $2 $3
