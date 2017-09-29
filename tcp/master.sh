
# Create Middleware server on lab1-2
ssh `whoami`@lab1-2.cs.mcgill.ca "bash -s" < ./cleanup.sh
ssh `whoami`@lab1-2.cs.mcgill.ca "bash -s" < ./servercode/create_middleware.sh

# Create Client Server on local instance
