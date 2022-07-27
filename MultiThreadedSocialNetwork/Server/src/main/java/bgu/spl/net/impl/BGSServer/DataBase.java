package bgu.spl.net.impl.BGSServer;

import bgu.spl.net.api.bidi.Connections;
import bgu.spl.net.impl.BGSServer.Messages.*;
import com.sun.org.apache.xerces.internal.impl.dv.util.HexBin;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class DataBase {
    // TODO: Make this class thread-safe if needed
    private static class SingletonHolder{
        private static final DataBase instance = new DataBase();
    }
    public static DataBase getInstance(){
        return SingletonHolder.instance;
    }

    private final MessageCode codeMap;
    private final Map<String, UserData> nameToUserData;
    private final Map<Integer, UserData> connectionIdToLoggedInUserData ;
    private final Set<UserData> loggedInUsers;

    private final Set<String> forbiddenWords;

    private DataBase(){
        codeMap = MessageCode.getInstance();
        //Two users may register concurrently
        nameToUserData = new ConcurrentHashMap<>();
        //Two users may log in or logout at the same time
        connectionIdToLoggedInUserData = new ConcurrentHashMap<>();
        //Two connections may be closed at the same time
        loggedInUsers = ConcurrentHashMap.newKeySet();

        forbiddenWords = ConcurrentHashMap.newKeySet();
        String[] words = {"filter", "China", "Shi", "Donald", "USA", "war"}; // Fill in your forbidden words
        forbiddenWords.addAll(Arrays.asList(words));
    }

    private boolean registered(String username){
        return nameToUserData.containsKey(username);
    }

    private boolean loggedIn(String username){
        return loggedInUsers.contains(nameToUserData.get(username));
    }

    private boolean loggedIn(int connectionId){
        return connectionIdToLoggedInUserData.get(connectionId) != null;
    }

    private UserData getLoggedInUserData(int connectionId){
        return connectionIdToLoggedInUserData.get(connectionId);
    }

    private UserData getUserData(String username){
        return nameToUserData.get(username);
    }

    //Returns false if user is already registered
    //TODO: Use synchronization to make sure registration completes properly...
    public boolean register(String username, String password, String birthday){
        // TODO: Can we assume a legal birthday?
        if(registered(username)){
            return false;
        }
        UserData data = new UserData(username, password, birthday);
        nameToUserData.put(username, data);
        return true;
    }

    //Returns false if login failed
    public boolean login(String username, String password, byte captcha, int connectionId, Connections<Message> connections){
        if(captcha == 0 || !registered(username) || loggedIn(username) || loggedIn(connectionId) || !getUserData(username).isPassword(password)){
            return false;
        }
        UserData data = getUserData(username);
        //TODO: Address possible synchronization problems between loggedIn,Registered to login,register
        //Update UserData's connectionId
        data.setConnectionId(connectionId);
        loggedInUsers.add(data);
        connectionIdToLoggedInUserData.put(connectionId, data);

        data.handleWaitingNotifications(connections);
        return true;
    }

    //Returns false if no user from 'connectionId' is logged in
    public boolean logout(int connectionId){
        if(loggedIn(connectionId)){
            UserData data = connectionIdToLoggedInUserData.remove(connectionId);
            loggedInUsers.remove(data);
            return true;
        }
        return false;
    }

    public boolean follow(String otherUsername, byte follow, int connectionId){
        //Note that if connectionId isn't valid data will be null.
        UserData myData = getLoggedInUserData(connectionId);
        UserData otherData = getUserData(otherUsername);

        //TODO: If 'a' blocks 'b', can either of them follow or unfollow the other?
        // TODO: If they can't, make sure that the use of 'follow' in 'block'
        //  happens before changing the blocked-sets
        if(loggedIn(connectionId) && registered(otherUsername) && (follow == 0)!=myData.follows(otherData) &&
                !myData.blocked(otherData) && !otherData.blocked(myData)){
            if (follow == 0) {
                myData.follow(otherData);
            }else{
                myData.unfollow(otherData);
            }
            return true;
        }
        return false;
    }

    public boolean post(String content, Connections<Message> connections, int connectionId){
        //TODO: Should posts be filtered as well?
        if(!loggedIn(connectionId)){
            return false;
        }
        //Note: In post we'll go over the message and look for usernames to send the message.
        //Note that if some tagged user isn't registered,
        // we'll just ignore him, continue on, and returning an ACK at the end.
        // (Unlike in STAT)

        UserData senderData = getLoggedInUserData(connectionId);
        String senderName = senderData.getUsername();
        NOTIFICATION notification = new NOTIFICATION((byte)1, senderName, content);
        UserData receiverData;
        String receiverName;

        int len = content.length();
        int start = -1;
        for(int i = 0; i < len; i++) {
            char c = content.charAt(i);
            if(c == '@'){
                start = i+1;
            }else if((c == ' ' || i == len-1) && start!=-1){
                // TODO: Synchronize follow status and blocks
                // TODO: What should we do if an illegal username is mentioned?
                //  Message all legal users and ignore him?
                if(i == len-1 && c != ' ') i++;
                receiverName = content.substring(start, i);
                if(registered(receiverName) &&
                        !senderData.blocked(receiverData = getUserData(receiverName)) &&
                        !receiverData.blocked(senderData) && !receiverData.follows(senderData)){
                    receiverData.deliverNotification(notification, connections);
                }
                start = -1;
            }
        }
        for(UserData follower : senderData.getFollowers()){
            follower.deliverNotification(notification, connections);
        }
        senderData.incrementNumOfPosts();
        return true;
    }

    public boolean PM(String username, String content, String sendingDateAndTime, Connections<Message> connections, int connectionId){
        UserData senderData;
        UserData receiverData;
        if(!loggedIn(connectionId) ||
                !registered((receiverData = getUserData(username)).getUsername()) ||
                !(senderData = getLoggedInUserData(connectionId)).follows(receiverData)) {
            return false;
        }
        NOTIFICATION notification = new NOTIFICATION((byte)0, senderData.getUsername(), filter(content) + " " + sendingDateAndTime);
        receiverData.deliverNotification(notification, connections);
        return true;
    }

    private String filter(String message){

        // We assume each forbidden word is just a word and not an expression
        // (Like 'West London' or 'Cold War')
        //TODO: Make sure to mention in the readMe.txt file that
        // we assume a message will not start with ' '.
        StringBuilder filteredMessage = new StringBuilder();
        String word;
        int len = message.length();
        int start = 0;
        for(int i = 0; i < len; i++){
            char c = message.charAt(i);
            if(c == ' ' || i == len-1){
                if (i==len-1 && c!=' ') i++;
                word = message.substring(start, i);
                if(forbiddenWords.contains(word)){
                    filteredMessage.append("<filtered>");
                }else{
                    filteredMessage.append(word);
                }
                while(i < len && message.charAt(i)==' '){
                    filteredMessage.append(" ");
                    i++;
                }
                start = i;
                i--;
            }
        }
        return filteredMessage.toString();
        /*
        int i;
        for(String word : forbiddenWords){
            if((i = message.indexOf(word)) >= 0){
                message = message.substring(0, i) + "<filtered>" + message.substring(i+word.length());
            }
        }

         */
    }

    public boolean logstat(Connections<Message> connections, int connectionId){
        // We can either include or exclude the sender data
        // We chose to include it
        if(loggedIn(connectionId)){
            UserData senderData = getLoggedInUserData(connectionId);
            for(UserData data : loggedInUsers){
                if(!data.blocked(senderData) && !senderData.blocked(data)){
                    connections.send(connectionId, data.getStatsAck(codeMap.getCode(LOGSTAT.class)));
                }
            }
            return true;
        }
        return false;
    }

    public boolean stat(String usernames, Connections<Message> connections, int connectionId){
        // We can either include or exclude the sender data
        // We chose to include it

        //Note: '|' only appears between two usernames
        //Note: We should we go through all the usernames and make sure
        // they are all registered and not-blocked before we start sending ACKs!
        ///If one of them is not, return an error message!!
        if(loggedIn(connectionId)){
            Queue<UserData> users = new LinkedList<>();
            UserData senderData = getLoggedInUserData(connectionId);
            UserData user;
            int i = 0;
            while(i < usernames.length()){
                int end = usernames.indexOf("|", i);
                if(end < 0){end = usernames.length();}
                user = getUserData(usernames.substring(i, end));
                if(user == null || user.blocked(senderData) || senderData.blocked(user)){return false;}
                users.add(user);
                i += user.getUsername().length()+1;
            }
            while(!users.isEmpty()){
                connections.send(connectionId, users.poll().getStatsAck(codeMap.getCode(STAT.class)));
            }
            return true;
        }
        return false;
    }

    public boolean block(String usernameToBlock, int connectionId){
        UserData sender;
        UserData other;
        if(loggedIn(connectionId) && registered(usernameToBlock) &&
                !(sender = getLoggedInUserData(connectionId)).blocked(other = getUserData(usernameToBlock))
                && !other.blocked(sender)){
            sender.block(other);
            return true;
        }
        return false;
    }

    //TODO: Should primitive variables be volatile for thread-safety(visibility)?
    private static class UserData{
        private final String username;
        private final String birthday;
        private final String hexHash; // Password hash in hex
        // Only one thread can change connectionId at a time
        // Changes only when the user logs in so there's no visibility problem(a new thread is opened)
        private int connectionId;

        private final AtomicInteger numOfPosts;

        private final Set<UserData> following; // Standard set (but we still use a concurrent set so we can have just the key set)
        private final Set<UserData> followers; //Concurrent set
        private final Set<UserData> blockedUsers; // Users I blocked
        private final Queue<NOTIFICATION> notificationsQueue;


        public UserData(String username, String password, String birthday){
            numOfPosts = new AtomicInteger();
            this.username = username;
            this.hexHash = getHexHash(password);
            this.birthday = birthday;
            this.following = ConcurrentHashMap.newKeySet();
            this.followers = ConcurrentHashMap.newKeySet();
            this.blockedUsers = ConcurrentHashMap.newKeySet();
            this.notificationsQueue = new ConcurrentLinkedQueue<>();
        }

        public String getUsername(){return username;}

        public void incrementNumOfPosts(){
            int val;
            do{
                val = numOfPosts.get();
            }while(!numOfPosts.compareAndSet(val, val+1));
        }

        public int getConnectionId(){return connectionId;}

        public void setConnectionId(int connectionId){
            this.connectionId = connectionId;
        }

        public ACK getStatsAck(short received_message_op_code){
            return new ACK(new Object[]{getInstance().codeMap.getCode(ACK.class), received_message_op_code, (short)getAge(), (short)numOfPosts.get(), (short)followers.size(), (short)following.size()});
        }

        public Set<UserData> getFollowers(){return followers;}

        public boolean isPassword(String password){
            return hexHash.equals(getHexHash(password));
        }

        public boolean follows(UserData other){
            return following.contains(other);
        }

        public boolean followedBy(UserData other){return followers.contains(other);}

        public synchronized void follow(UserData other){
            following.add(other);
            other.followers.add(this);
        }

        //Block and Unfollow can not retrieve a problem
        public void unfollow(UserData other){
            following.remove(other);
            other.followers.remove(this);
        }

        //TODO: Check synchronization

        // Did I block him?
        public boolean blocked(UserData other){
            return blockedUsers.contains(other);
        }

        public void block(UserData toBlock){
            blockedUsers.add(toBlock);
            this.unfollow(toBlock);
            //Synchronize follow-unfollow
            synchronized (toBlock){
                toBlock.unfollow(this);
            }
        }

        public void handleWaitingNotifications(Connections<Message> connections){
            while(!notificationsQueue.isEmpty()){
                //TODO: Should we check the validity of the notification?
                // For example if the receiver doesn't follow the sender anymore or if either of them blocked the other
                connections.send(getConnectionId(), notificationsQueue.poll());
            }
        }

        public void addWaitingNotification(NOTIFICATION notification){
            notificationsQueue.add(notification);
        }

        // Called from receiverData
        public void deliverNotification(NOTIFICATION notification, Connections<Message> connections){
            //TODO: Synchronize notification sending and logout
            if(DataBase.getInstance().loggedIn(getUsername())){
                connections.send(getConnectionId(), notification);
            }else{
                //TODO: Should we keep a time-stamp for each message
                // in the notification queue to be updated with the
                // changes in the blocking and follow statuses?
                addWaitingNotification(notification);
            }
        }

        private int getAge(){
            int day = Integer.parseInt(birthday.substring(0, 2));
            int month = Integer.parseInt(birthday.substring(3, 5));
            int year = Integer.parseInt(birthday.substring(6, 10));

            //Since Java 8
            Date date = new Date();
            LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            int curYear  = localDate.getYear();
            int curMonth = localDate.getMonthValue();
            int curDay   = localDate.getDayOfMonth();

            int age = curYear - year;
            //If he didn't have his birthday this year yet
            if(month > curMonth || (month == curMonth && day > curDay)){
                age--;
            }
            return age;
        }

        private static String getHexHash(String text){
            try {
                byte[] hash = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
                return HexBin.encode(hash);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
