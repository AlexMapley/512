kill -9 $(lsof +D `pwd` | awk '!/bash/' | awk '!/lsof/' | awk '{print $2}')
kill -9 $(ps -aux | grep ".nfs" | awk '{print $2}')
rm .nfs*
rm inPipe*
rm outLog*
rm ../servercode/metrics.txt
