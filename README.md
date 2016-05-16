# Simple-Dictionary-Client

Implemention of a dictionary client in Java to retrieve definitions from a dictionary server using application-level protocol as per RFC 2229. 

Commands:


1. open $server $port

2. dict

3. set $dictionary

4. currdict

5. define $word

6. match $word 

7. prefixmatch $word

8. close

9. quit


Description of commands:

1. dictionary server name is either domain name or IP address in dotted form. $port is optional, default 2628
2. retrieve and show the list of all dictionaries the server supports
3. set the current dictionary
4. prints name of current dictionary, initially "*"
5. retrieve and prints all definitions for given word in the current dictionary
6. retrieve and print all exact matches for given word
7. same as above but prefix matching
8. closes connection to dict server
9. closes connection to dict server and exits program



