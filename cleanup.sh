rm servercode/ResImpl/*.class
rm *.swp
rm servercode/*.jar
kill -9 $(lsof +D `pwd` | grep ".nfs" | awk '{print $2}')
