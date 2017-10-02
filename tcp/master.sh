# Create RM server ons lab1-4,5,6
ssh `whoami`@lab1-4.cs.mcgill.ca "bash -s" < ./cleanup.sh
ssh `whoami`@lab1-4.cs.mcgill.ca "bash -s" < ./servercode/create_server.sh
ssh `whoami`@lab1-5.cs.mcgill.ca "bash -s" < ./cleanup.sh
ssh `whoami`@lab1-5.cs.mcgill.ca "bash -s" < ./servercode/create_server.sh
ssh `whoami`@lab1-6.cs.mcgill.ca "bash -s" < ./cleanup.sh
ssh `whoami`@lab1-6.cs.mcgill.ca "bash -s" < ./servercode/create_server.sh

# Create Middleware server on lab1-2
ssh `whoami`@lab1-7.cs.mcgill.ca "bash -s" < ./cleanup.sh
ssh `whoami`@lab1-7.cs.mcgill.ca "bash -s" < ./servercode/create_middleware.sh

# Create Client Server on local instance
ssh `whoami`@lab1-1.cs.mcgill.ca "bash -s" < ./cleanup.sh
ssh `whoami`@lab1-1.cs.mcgill.ca "bash -s" < ./servercode/create_middleware.sh
