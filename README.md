# Simple-Dictionary-Client

Implemention of a dictionary client in Java to retrieve definitions from a dictionary server using application-level protocol as per RFC 2229. 

Commands:


open $server $port    -   dictionary server name is either domain name or IP address in dotted form. $port is optional, default 2628

dict                  -   retrieve and show the list of all dictionaries the server supports

set $dictionary       -   set the current dictionary

currdict              -   prints name of current dictionary, initially "*"

define $word          -   retrieve and prints all definitions for given word in the current dictionary

match $word           -   retrieve and print all exact matches for given word

prefixmatch $word     -   same as above but prefix matching

close                 -   closes connection to dict server

quit                  -   closes connection to dict server and exits program


