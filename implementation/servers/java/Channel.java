import java.util.ArrayList;
import java.util.stream.Stream;
import java.util.stream.Collectors;

class Channel
{
    // channel name
    private String name = "";

    // ctor
    public Channel(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        // return channel name
        return this.name;
    }

    public void sendNicklist(User target)
    {
        ArrayList<User> users = Globals.server.getChannelUsers(this.name);

        String nicklist = users.stream().map(u -> u.getName()).collect(Collectors.joining(" "));

        target.sendMessage(353, target.getName() + " @ " + this.name, nicklist);
        target.sendMessage(366, target.getName() + " " + this.name, "END of /NAMES list.");
    }

    public void sendMessage(String source, String message)
    {
        Globals.server.sendChannelMessage(this.name, source, "PRIVMSG", this.name, message, false);
    }

    public void sendAll(String source, String type, String param, String message)
    {
        // send message to all channel members
        Globals.server.sendChannelMessage(this.name, source, type, param, message, true);
    }

    public void handleJoin(User user)
    {
        // send JOIN message to all channel members
        Globals.server.sendChannelMessage(this.name, user.getIdent(), "JOIN", this.name, "", true);

        // acknowledge join to user
        user.sendMessage(user.getIdent(), "JOIN", this.name, "");

        Globals.server.addChannelUser(this.name, user);

        sendNicklist(user);
    }

    public void handlePart(User user)
    {
        // send PART message to all channel members including sender 
        Globals.server.sendChannelMessage(this.name, user.getIdent(), "PART", this.name, "", true);
        Globals.server.removeChannelUser(this.name, user);
    }

    public void handleNick(User user, String nick)
    {
        // notify all channel members of nick change
        Globals.server.sendChannelMessage(this.name, user.getName(), "NICK", nick, "", true);
    }

    public void handleQuit(User user, String reason)
    {
        // tell all channel members that 'user' quitted 
        Globals.server.sendChannelMessage(this.name, user.getIdent(), "QUIT", "", reason, true);
        Globals.server.removeChannelUser(this.name, user);
    }
}
