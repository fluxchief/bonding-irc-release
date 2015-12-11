import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.stream.Collectors;

class User
{
    // buffer of to-be-sent messages
    private ArrayList<String> outBuffer = new ArrayList<>();

    // SocketChannel belonging to the user
    private SocketChannel socketChannel = null;

    // information about user
    private String nickname = "";
    private String hostname = "";
    private String realname = "";
    private String username = "";

    // list of channels the user has joined
    private ArrayList<String> channels = new ArrayList<>();

    // ctor
    public User(SocketChannel socketChannel)
    {
        this.socketChannel = socketChannel;
    }

    public void setup(int userId)
    {
        // initalize user data with default values
        this.username = "user" + userId;
        this.nickname = "user" + userId;
        this.hostname = getRemoteAddress();

        // send some information about the server
        sendWelcomeMessage();
    }

    public void execute(Command cmd)
    {
        if (cmd.command.equals(""))
        {
            // job done, no command passed
            return;
        }

        // call handlers according to command
        switch (cmd.command)
        {
            case "PING":    handlePing(cmd.arguments);                        break;
            case "NICK":    handleNick(cmd.arguments);                        break;
            case "PART":    handlePart(cmd.arguments);                        break;
            case "JOIN":    handleJoin(cmd.arguments);                        break;
            case "PRIVMSG": handlePrivMsg(cmd.arguments);                     break;
            case "QUIT":    handleQuit(cmd.arguments);                        break;
            default:        sendMessage(421, cmd.command, "Unknown command"); break;
        }
    }

    public String getName()
    {
        // return the user's current nickname
        return this.nickname;
    }

    public String getIdent()
    {
        // build and return the user identifier
        return this.nickname + "!x@" + this.hostname;
    }

    public SocketChannel getSocketChannel()
    {
        // return the user's SocketChannel
        return this.socketChannel;
    }

    public void sendToUser(String msg)
    {
        // add new message to to-be-sent buffer
        this.outBuffer.add(msg);
    }

    public String getNextOutgoingMessage()
    {
        // return the next to-be-sent message and remove it from buffer
        String line = outBuffer.get(0);
        outBuffer.remove(0);

        return line;
    }

    public boolean hasOutgoingMessage()
    {
        // check whether there are some messages left in the send queue
        return this.outBuffer.size() > 0;
    }

    public void sendPrivateMessage(String source, String message)
    {
        // send private message to user
        sendMessage(source, "PRIVMSG", this.nickname, message);
    }

    public void sendWelcomeMessage()
    {
        // send some welcome message to user,
        // these messages can be anything
        sendMessage(1, this.nickname, "Welcome");
        sendMessage(375, this.nickname, "Start MOTD");
        sendMessage(376, this.nickname, "End of MOTD");
    }

    public Command parseLine(String line)
    {
        if (line.length() == 0)
        {
            // emtpy lines means no command
            return new Command();
        }

        // some temporary variables
        String command = "";
        String t_parameter = "";
        ArrayList<String> parameter = new ArrayList<>();

        // trim line
        line = line.trim();

        // extract the actual command from the message line
        int pos = line.indexOf(" ");
        if (pos != -1) 
        {
            command = line.substring(0, pos);
            t_parameter = line.substring(pos + 1);
        } 
        else 
        {
            command = line;
        }

        // convert the single line of parameters to a list of parameters;
        // arguments are separated by spaces, if there's
        // any message shipped with the command, the first colon is the 
        // start of the message and all other colons will be ignored afterwards 
        if(!t_parameter.equals("")) 
        {
            pos = t_parameter.indexOf(" ");

            while (pos != -1 && t_parameter.charAt(0) != ':') 
            {
                String param = t_parameter.substring(0, pos).trim();
                
                parameter.add(param);

                t_parameter = t_parameter.substring(pos + 1).trim();
                pos = t_parameter.indexOf(" ");
            }

            if(!t_parameter.isEmpty() && t_parameter.charAt(0) == ':')
                t_parameter = t_parameter.substring(1);

            parameter.add(t_parameter.trim());
        }

        // convert command to uppercase
        command = command.toUpperCase();


        if (!command.equals(""))
        {
            // convert args to a printable format
            String args = "['" + parameter.stream().collect(Collectors.joining("', '")) + "']";

            System.out.println("Received command: ['" + command + "'] " + args);
        }

        return new Command(command, parameter);
    }

    public void sendMessage(int code, String param, String message)
    {
        // send server message/response to client
        sendMessage(Globals.server.getServerName(), String.format("%03d", code), param, message);
    }

    public void sendMessage(String source, String type, String param, String message)
    {
        // build a valid message and add it to send queue
        String buf = "";
        buf += ":" + source + " " + type + " " + param;

        if(!message.isEmpty())
            buf += " :" + message;
        
        buf += "\n";

        sendToUser(buf);
    }

    public String getRemoteAddress()
    {
        // retrieve remote address of client, (mostly) needed for logging
        try
        {
            return ((InetSocketAddress)this.socketChannel.getRemoteAddress()).getHostName();
        }
        catch (IOException e)
        {
            System.out.println("Something went horribly wrong :(");
            e.printStackTrace();

            System.exit(1);
        }

        return null;
    }

    public void handlePing(ArrayList<String> parameters)
    {
        if (parameters.size() == 0)
        {
            sendMessage(432, "Unset", "Not enough parameters for PING");

            System.out.println("Not enough parameters for PING from " + getRemoteAddress());

            return;
        }
        // reply to ping message
        sendMessage(Globals.server.getServerName(), "PONG", parameters.get(0), "");
    }

    public void handlePart(ArrayList<String> parameters)
    {
        if (parameters.size() == 0)
        {
            sendMessage(432, "Unset", "Not enough parameters for PART");

            System.out.println("Not enough parameters for PART from " + getRemoteAddress());

            return;
        }

        // get channel the user wants to PART from and send PART message
        // also, remove the channel from the user's channel list
        Globals.server.getChannel(parameters.get(0)).handlePart(this);
        this.channels.remove(parameters.get(0));
    }

    public void handleNick(ArrayList<String> parameters)
    {
        // validate requested nick name
        ArrayList<String> illegalChars = new ArrayList<>();
        illegalChars.add(" "); // arguments splitter in raw commands, thus not allowed
        illegalChars.add("@"); // part of user ident, separates hostname from username
        illegalChars.add("!"); // splits nickname and username in ident

        // NICK needs at least one parameter (any other arguments will be ignored)
        if (parameters.size() == 0)
        {
            sendMessage(432, "Unset", "Not enough paramters for NICK");

            System.out.println("Not enough parameters for NICK from " + getRemoteAddress());

            return;
        }

        // get requested nick
        String nick = parameters.get(0);

        // check of there's one or more of the illegal characters in it
        boolean validNick = illegalChars.stream().filter(c -> nick.indexOf(c) > -1).collect(Collectors.toList()).size() == 0;

        // channel names start with a '#' character, therefore nicknames are not allowed to
        if (nick.startsWith("#"))
        {
            validNick = false;
        }

        // nickname not allowed for some of the reasons we check for above
        if (!validNick)
        {
            sendMessage(432, nick, "Erroneous nickname");

            System.out.println("Erroneous nickname from " + getRemoteAddress());

            return;
        }

        // check if there's already a user with the same nick
        if (Globals.server.getUserByNick(nick) != null)
        {
            sendMessage(433, nick, "Nickname is already in use");

            System.out.println("Nickname already in by from " + getRemoteAddress());

            return;
        }

        // nick seems valid; acknowledge nickchange to client
        sendMessage(getIdent(), "NICK", parameters.get(0), "");

        // announce the nickchange in each of the user's channels
        for (String name : this.channels)
        {
            Globals.server.getChannel(name).handleNick(this, nick);
        }

        this.nickname = nick;
    }

    public void handleJoin(ArrayList<String> parameters)
    {
        // JOIN needs at least one argument, any other arguments will be ignored
        if (parameters.size() == 0)
        {
            sendMessage(432, "Unset", "Not enough parameters for JOIN");

            System.out.println("Not enough parameters for JOIN from " + getRemoteAddress());

            return;
        }

        if (!parameters.get(0).startsWith("#"))
        {
            sendMessage(432, "Unset", "Channel names have to start with #");

            System.out.println("Channel name format not correct by " + getRemoteAddress());

            return;
        }

        // get first argument as channel name and try to retrieve
        // a channel with that name from the server
        String channelName = parameters.get(0);
        Channel channel = Globals.server.getChannel(channelName);

        // channel does not exist on the server
        // ask the server to create it
        if (channel == null)
        {
            channel = Globals.server.createChannel(channelName);
        }

        // add channel to user's channel list
        this.channels.add(channelName);

        // tell the channel that a new user has joined
        channel.handleJoin(this);
    }

    public void handlePrivMsg(ArrayList<String> parameters)
    {
        // PRIVMSG needs exact two arguments (receiver, message)
        if (parameters.size() != 2)
        {
            sendMessage(432, "Unset", "Not enough parameters for PRIVMSG");

            System.out.println("Not enough parameters for PRIVMSG from " + getRemoteAddress());

            return;
        }

        String target = parameters.get(0);

        // both a channel or a user can be receiver of a PRIVMSG,
        // so we need to distinguish. a channel will be more common, though,
        // therefore we start checking for the channel and afterwards for a user
        if (target.startsWith("#"))
        {
            Globals.server.sendChannelMessage(target, getIdent(), "PRIVMSG", target, parameters.get(1), false);
        }
        else
        {
            Globals.server.sendPrivateMessage(target, getIdent(), parameters.get(1));
        }
    }

    public void handleQuit(ArrayList<String> parameters)
    {
        // default quit reason
        String reason = "Quit";

        // a QUIT message can optionally have a reason
        // if it has, overwrite the default
        if (parameters.size() == 1)
        {
            reason = parameters.get(0);
        }

        // tell every of the user's channel that it quitted
        for (String name : this.channels)
        {
            Globals.server.getChannel(name).handleQuit(this, reason);
        }

        // remove all channels from the user's list 
        // and remove the client from the server
        this.channels.clear();
        Globals.server.removeUser(this);
    }
}
