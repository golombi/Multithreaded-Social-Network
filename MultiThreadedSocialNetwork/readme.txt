RUN COMMANDS: first start server, than enter clients

Server: (From Server folder)
    compile - mvn compile
    TCP:
        mvn exec:java -Dexec.mainClass="bgu.spl.net.impl.BGSServer.TPCMain" -Dexec.args="<port>"
            Example: mvn exec:java -Dexec.mainClass="bgu.spl.net.impl.BGSServer.TPCMain" -Dexec.args="7777"
    Reactor
        mvn exec:java -Dexec.mainClass="bgu.spl.net.impl.BGSServer.ReactorMain" -Dexec.args="<port> <num of worker threads>"
            Example: mvn exec:java -Dexec.mainClass="bgu.spl.net.impl.BGSServer.ReactorMain" -Dexec.args="7777 20"

Client: (From Client/bin/ folder)
        compile - make
        ./BGSclient <ip> <port>
            Example: BGSclient 127.0.0.1 7777


Messages:
REGISTER - REGISTER <username> <password> <birthday>
    REGISTER ExampleUser pass123 07-11-1985
    //date format: DD-MM-YYYY

LOGIN - LOGIN <username> <password> <captcha>
    LOGIN ExampleUser pass123 1
    //username cannot contain spaces, password cannot contain spaces

LOGOUT - LOGOUT

FOLLOW - FOLLOW <0/1 (Follow/Unfollow)> <UserName>
    FOLLOW 0 ExampleUser

POST - POST <PostMsg>
    POST hello followers! // POST hello @another my friends @another2

PM - PM <Username> <Content>
    PM ExampleUser hey you

LOGSTAT - LOGSTAT

STAT - STAT <UserNames_list>
    STAT userA|userB|userC

BLOCK - BLOCK <username>
    BLOCK ExampleUser


FILTERED WORDS:
The filtered words can be found as an array - String[]words = {"filter", "China", "Shi", "Donald", "USA", "war"}
in the constructor of the class DataBase: bgu.spl.net.impl.BGSServer.DataBase
