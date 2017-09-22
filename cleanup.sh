rm servercode/ResImpl/*.class
rm .*swp
rm servercode/*.jar
kill -9 $(ps -aux | grep ".nfs" | awk '{print $2}')
kill -9 $(ps -aux | grep "rmiregistry -J" | awk '{print $2}')

