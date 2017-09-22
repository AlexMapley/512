# Register RMI
rmiregistry -J-Djava.rmi.server.useCodebaseOnly=false 1099 &

# Exporting Class Path
ClassPath="`pwd`""/servercode"
echo $ClassPath
export CLASSPATH="`pwd`""/servercode"

# Compiling codebase
javac servercode/ResInterface/ResourceManager.java
jar cvf servercode/ResInterface.jar servercode/ResInterface/*.class
javac servercode/ResImpl/ResourceManagerImpl.java

# Setting permissions
chmod 704 servercode/ResInterface.jar
chmod 704 servercode/ResInterface/*.class
chmod 705 servercode/* # directory needs to be executable???

# Run server on registered instance
java -Djava.security.policy=servercode/java.policy -Djava.rmi.server.codebase=file:`pwd`/servercode/ ResImpl.ResourceManagerImpl 1099
