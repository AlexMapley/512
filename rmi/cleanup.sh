rm servercode/ResImpl/*.class
rm .*swp
rm *.jar
rm .nfs*
kill -9 $(ps -aux | grep ".nfs" | awk '{print $2}')
kill -9 $(ps -aux | grep "rmiregistry -J" | awk '{print $2}')
kill -9 $(ps -aux | grep "gbryan" | awk '{print $2}')
kill -9 $(ps -aux | grep "amaple" | awk '{print $2}')
