# Exporting Class Path
ClassPath="`pwd`""/servercode"
echo $ClassPath
export CLASSPATH="`pwd`""/servercode"

#Compiling codebase
javac servercode/ResInterface/ResourceManager.java
jar cvf servercode/ResInterface.jar servercode/ResInterface/*.class
javac servercode/ResImpl/ResourceManagerImpl.java
