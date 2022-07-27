#include <stdlib.h>
#include <connectionHandler.h>

#include <thread>
#include <string>
#include <condition_variable>

using namespace std;

std::condition_variable cv;
std::mutex m;

std::string process(std::string basicString);

void readFromSocketTask(ConnectionHandler* handler, int* key){
    while(true) {
        std::string answer;
        // Get back an answer: by using the expected number of bytes (len bytes + newline delimiter)
        // We could also use: connectionHandler.getline(answer) and then get the answer without the newline char at the end
        if (!handler->getLine(answer)) {
            std::cout << "Disconnected. Exiting...\n" << std::endl;
            return;
        }


        std::cout << answer << std::endl;
        //if sent logout
        std::size_t found =  answer.find("ACK 3");
        if (found != string::npos) {
            std::cout << "Exiting...\n" << std::endl;
            *key = 1;
            cv.notify_all();
            return;
        }
        found =  answer.find("ERROR 3");
        if(found != string::npos) {
            *key = -1;
            cv.notify_all();
        }
    }
}

void writeToSocketTask(ConnectionHandler* handler, int* key){
    std::unique_lock<std::mutex> lk(m);
    while (true) {
        const short bufsize = 1024;
        char buf[bufsize];
        std::cin.getline(buf, bufsize);
        std::string line(buf);

        if (!handler->sendLine(line)) {
            std::cout << "Disconnected. Exiting...\n" << std::endl;
            break;
        }
        std::size_t index;
        if(((index = line.find("LOGOUT")) != string::npos && index == 0)){
            cv.wait(lk); // waits until notified
            if(*key == -1) *key = 0;
            else {
                std::cout << "Disconnected. Exiting...\n" << std::endl;
                break;
            }
        }

    }
}




/**
* This code assumes that the server replies the exact text the client sent it (as opposed to the practical session example)
*/
int main (int argc, char *argv[]) {
    if (argc < 3) {
        std::cerr << "Usage: " << argv[0] << " host port" << std::endl << std::endl;
        return -1;
    }
    std::string host = argv[1];
    short port = atoi(argv[2]);
    
    ConnectionHandler connectionHandler(host, port);
    if (!connectionHandler.connect()) {
        std::cerr << "Cannot connect to " << host << ":" << port << std::endl;
        return 1;
    }

    int* key = new int(0);

    std::thread th1(writeToSocketTask, &connectionHandler, key);
    std::thread th2(readFromSocketTask, &connectionHandler,key);

    th1.join();
    th2.join();
    delete key;
    return 0;
}


