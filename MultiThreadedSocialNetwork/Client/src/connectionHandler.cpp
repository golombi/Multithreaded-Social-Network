#include <connectionHandler.h>

using boost::asio::ip::tcp;

using std::cin;
using std::cout;
using std::cerr;
using std::endl;
using std::string;

ConnectionHandler::ConnectionHandler(string host, short port) : host_(host), port_(port), io_service_(),
                                                                socket_(io_service_) {}

ConnectionHandler::~ConnectionHandler() {
    close();
}

bool ConnectionHandler::connect() {
    std::cout << "Starting connect to "
              << host_ << ":" << port_ << std::endl;
    try {
        tcp::endpoint endpoint(boost::asio::ip::address::from_string(host_), port_); // the server endpoint
        boost::system::error_code error;
        socket_.connect(endpoint, error);
        if (error)
            throw boost::system::system_error(error);
    }
    catch (std::exception &e) {
        std::cerr << "Connection failed (Error: " << e.what() << ')' << std::endl;
        return false;
    }
    return true;
}


void shortToBytes(short num, int startIn, char *bytesArr) {
    bytesArr[startIn] = ((num >> 8) & 0xFF);
    bytesArr[startIn + 1] = (num & 0xFF);
}


short bytesToShort(char *bytesArr) {
    short result = (short) ((bytesArr[0] & 0xff) << 8);
    result += (short) (bytesArr[1] & 0xff);
    return result;
}


bool ConnectionHandler::getBytes(char bytes[], unsigned int bytesToRead) {
    size_t tmp = 0;
    boost::system::error_code error;
    try {
        while (!error && bytesToRead > tmp) {
            tmp += socket_.read_some(boost::asio::buffer(bytes + tmp, bytesToRead - tmp), error);
        }
        if (error)
            throw boost::system::system_error(error);
    } catch (std::exception &e) {
        std::cerr << "recv failed (Error: " << e.what() << ')' << std::endl;
        return false;
    }
    return true;
}

bool ConnectionHandler::sendBytes(const char bytes[], int bytesToWrite) {
    int tmp = 0;
    boost::system::error_code error;
    try {
        while (!error && bytesToWrite > tmp) {
            tmp += socket_.write_some(boost::asio::buffer(bytes + tmp, bytesToWrite - tmp), error);
        }
        if (error)
            throw boost::system::system_error(error);
    } catch (std::exception &e) {
        std::cerr << "recv failed (Error: " << e.what() << ')' << std::endl;
        return false;
    }
    return true;
}

bool ConnectionHandler::getLine(std::string &line) {
    return getFrameAscii(line, ';');
}

bool ConnectionHandler::sendLine(std::string &line) {
    return sendFrameAscii(line, ';');
}

bool ConnectionHandler::getFrameAscii(std::string &frame, char delimiter) {
    char code[2];
    getBytes(code, 2);
    if (bytesToShort(code) == 9) {
        char type;
        getBytes(&type, 1);
        if (type == '\1') frame.append("NOTIFICATION Public ");
        else frame.append("NOTIFICATION PM ");
        //get message
        char ch;
        // Stop when we encounter the null character.
        // Notice that the null character is not appended to the frame string.
        try {
            do {
                getBytes(&ch, 1);
                if (ch == delimiter) break;
                if (ch == '\0') frame.append(1, ' ');
                else frame.append(1, ch);
            } while (delimiter != ch);
        } catch (std::exception &e) {
            std::cerr << "recv failed (Error: " << e.what() << ')' << std::endl;
            return false;
        }
    } else if (bytesToShort(code) == 10) {
        char messageOpt[2];
        getBytes(messageOpt, 2);
        std::string error = "ACK ";
        frame.append(error);
        frame.append(std::to_string((int) bytesToShort(messageOpt)));

        //get message in bytes
        if (bytesToShort(messageOpt) == 4) {
            frame.append(" ");
            char ch;
            try {
                do {
                    getBytes(&ch, 1);
                    if (ch == delimiter) break;
                    if (ch == '\0') continue;
                    else frame.append(1, ch);
                } while (delimiter != ch);
            } catch (std::exception &e) {
                std::cerr << "recv failed (Error: " << e.what() << ')' << std::endl;
                return false;
            }
        } else if (bytesToShort(messageOpt) == 7 || bytesToShort(messageOpt) == 8) {
            char content[2];
            char ch;
            int index = 0;
            try {
                do {
                    getBytes(&ch, 1);
                    if (ch == delimiter) break;
                    content[index] = ch;
                    if (index == 1) {
                        frame.append(" ");
                        frame.append(std::to_string((int) bytesToShort(content)));
                        index = 0;
                    } else index++;
                } while (ch != delimiter);
            } catch (std::exception &e) {
                std::cerr << "recv failed (Error: " << e.what() << ')' << std::endl;
                return false;
            }
        }else{
            char c; //get ;
            getBytes(&c, 1);
        }

    } else if (bytesToShort(code) == 11) {

        char messageOpt[2];
        getBytes(messageOpt, 2);
        std::string error = "ERROR ";
        frame.append(error);
        frame.append(std::to_string((int) bytesToShort(messageOpt)));
        char ch; //get ;
        getBytes(&ch, 1);
    }

    return true;
}

bool ConnectionHandler::sendFrameAscii(const std::string &frame, char delimiter) {
    std::size_t index;

    if ((index = frame.find("REGISTER")) != string::npos && index == 0) {
        char bytesArr[frame.size() + 3 - 9]; //new message
        shortToBytes(1, 0, bytesArr);
        std::size_t i = 9; //check
        for (int byteIndex = 2; i < frame.size(); byteIndex++, i++) {
            if (frame[i] == ' ') {
                bytesArr[byteIndex] = '\0';
            } else {
                bytesArr[byteIndex] = frame[i];
            }
        }
        bytesArr[sizeof(bytesArr) / sizeof(*bytesArr) - 1] = '\0';

        bool result = sendBytes(bytesArr, sizeof(bytesArr) / sizeof(*bytesArr));
        if (!result) return false;
        return sendBytes(&delimiter, 1);

    } else if ((index = frame.find("LOGIN")) != string::npos && index == 0) {
        char bytesArr[frame.size() - 6 + 2]; //new message
        shortToBytes(2, 0, bytesArr);
        std::size_t i = 6; //check
        int id = 0;
        for (int byteIndex = 2; i < frame.size(); byteIndex++, i++) {
            if (frame[i] == ' ') {
                bytesArr[byteIndex] = '\0';
                id++;
            } else {
                if (id == 2) {
                    if (frame[i] == '1') bytesArr[byteIndex] = '\1';
                    else if (frame[i] == '0') bytesArr[byteIndex] = '\0';
                } else bytesArr[byteIndex] = frame[i];
            }
        }

        bool result = sendBytes(bytesArr, sizeof(bytesArr) / sizeof(*bytesArr));
        if (!result) return false;
        return sendBytes(&delimiter, 1);


    } else if ((index = frame.find("LOGOUT")) != string::npos && index == 0) {
        char bytesArr[2];
        shortToBytes(3, 0, bytesArr);

        bool result = sendBytes(bytesArr, sizeof(bytesArr) / sizeof(*bytesArr));
        if (!result) return false;
        return sendBytes(&delimiter, 1);
    } else if ((index = frame.find("FOLLOW")) != string::npos && index == 0) {
        char bytesArr[frame.size() + 2 - 7]; //message length
        shortToBytes(4, 0, bytesArr); //optcode
        //bytesArr[2] = frame[7]; //follow/unfollow
        if(frame[7] == '1') bytesArr[2] = '\1';
        else if(frame[7] == '0') bytesArr[2] = '\0';

        for (std::size_t i = 9, byteIndex = 3; i < frame.size(); byteIndex++, i++) {
            bytesArr[byteIndex] = frame[i];
        }
        int len = sizeof(bytesArr) / sizeof(*bytesArr);
        bytesArr[len - 1] = '\0';

        bool result = sendBytes(bytesArr, len);
        if (!result) return false;
        return sendBytes(&delimiter, 1);

    } else if ((index = frame.find("POST")) != string::npos && index == 0) {
        char bytesArr[frame.size() + 3 - 5];
        shortToBytes(5, 0, bytesArr); //optcode
        for (std::size_t i = 5, byteIndex = 2; i < frame.size(); byteIndex++, i++) { //message to post
            bytesArr[byteIndex] = frame[i];
        }
        bytesArr[sizeof(bytesArr) / sizeof(*bytesArr) - 1] = '\0';

        bool result = sendBytes(bytesArr, sizeof(bytesArr) / sizeof(*bytesArr));
        if (!result) return false;
        return sendBytes(&delimiter, 1);


    } else if ((index = frame.find("PM")) != string::npos && index == 0) {

        //add date
        auto t = std::time(nullptr);
        auto tm = *std::localtime(&t);
        std::ostringstream oss;
        oss << std::put_time(&tm, "%d-%m-%Y %H:%M");
        auto str = oss.str();

        char bytesArr[frame.size() + 4 - 3 + str.size()];
        shortToBytes(6, 0, bytesArr);

        bool username = true;
        int byteIndex = 2;
        for (std::size_t i = 3; i < frame.size(); byteIndex++, i++) {
            if (username && frame[i] == ' ') {
                bytesArr[byteIndex] = '\0';
                username = false;
                continue;
            }
            bytesArr[byteIndex] = frame[i];
        }

        bytesArr[byteIndex] = '\0';
        byteIndex++;

        for (std::size_t i = 0; i < str.size(); byteIndex++, i++) {
            bytesArr[byteIndex] = str[i];
        }

        bytesArr[byteIndex] = '\0';

        bool result = sendBytes(bytesArr, sizeof(bytesArr) / sizeof(*bytesArr));
        if (!result) return false;
        return sendBytes(&delimiter, 1);

    } else if ((index = frame.find("LOGSTAT")) != string::npos && index == 0) {
        char bytesArr[2];
        shortToBytes(7, 0, bytesArr);

        bool result = sendBytes(bytesArr, sizeof(bytesArr) / sizeof(*bytesArr));
        if (!result) return false;
        return sendBytes(&delimiter, 1);
    } else if ((index = frame.find("STAT")) != string::npos && index == 0) {
        char bytesArr[frame.size() + 3 - 5];
        shortToBytes(8, 0, bytesArr);
        for (std::size_t i = 5, byteIndex = 2; i < frame.size(); byteIndex++, i++) { //message to post
            bytesArr[byteIndex] = frame[i];
        }
        bytesArr[sizeof(bytesArr) / sizeof(*bytesArr) - 1] = '\0';

        bool result = sendBytes(bytesArr, sizeof(bytesArr) / sizeof(*bytesArr));
        if (!result) return false;
        return sendBytes(&delimiter, 1);


    } else if ((index = frame.find("BLOCK")) != string::npos && index == 0) {
        char bytesArr[frame.size() + 3 - 6]; //message length
        shortToBytes(12, 0, bytesArr); //optcode
        for (std::size_t i = 6, byteIndex = 2; i < frame.size(); byteIndex++, i++) { //message to post
            bytesArr[byteIndex] = frame[i];
        }
        bytesArr[sizeof(bytesArr) / sizeof(*bytesArr) - 1] = '\0';

        bool result = sendBytes(bytesArr, sizeof(bytesArr) / sizeof(*bytesArr));
        if (!result) return false;
        return sendBytes(&delimiter, 1);
    }

    return true;
}

// Close down the connection properly.
void ConnectionHandler::close() {
    try {
        socket_.close();
    } catch (...) {
        std::cout << "closing failed: connection already closed" << std::endl;
    }
}
