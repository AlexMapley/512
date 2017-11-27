printf "
\n\n\n\n##################################################################
\n# Create RM server ons lab1-4,5,6
\nssh `whoami`@lab1-4.cs.mcgill.ca

\nssh `whoami`@lab1-5.cs.mcgill.ca

\nssh `whoami`@lab1-6.cs.mcgill.ca

\n

\n# Create Middleware server on lab1-2
\nssh `whoami`@lab1-2.cs.mcgill.ca
\n
\n# Create Client Servers on lab2-*
\nssh `whoami`@lab2-1.cs.mcgill.ca 
\n\n\n\n##################################################################
\n"
