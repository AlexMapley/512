rm servercode/ResImpl/*.class
rm .*swp
rm *.jar
rm .nfs*
rm clientsrc/*.jar
rm servercode/*.jar
rm servercode/ResImpl/*.jar
kill -9 $(ps -aux | grep ".nfs" | awk '{print $2}')
kill -9 $(ps -aux | grep "rmiregistry -J" | awk '{print $2}')
