import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ArrayList;

class Server
{
    // needed for handling the SocketChannels in non-blocking mode
    private Selector selector;

    // the server's socket channel from which we get
    // all the client SocketChannels we need
    private ServerSocketChannel server;

    // map each SocketChannel to a specific user
    private Map<SocketChannel, User> clients = new HashMap<>();

    // list of channels that are available on the server
    private ArrayList<Channel> channels = new ArrayList<>();

    // list of user per channel
    private HashMap<String, ArrayList<User>> channelUsers = new HashMap<>();

    // user counter, needed for initial unique user name
    // until the clients sends some
    private int userCounter = 0;

    // the server's name
    private final String serverName = "FluxIRCServer";

    // ctor
    public Server(String host, int port)
    {
        try
        {
            InetSocketAddress sockaddr = new InetSocketAddress(host, port);

            // open a ServerSocketChannel in non-blocking mode and bind it
            // to the given address and port
            this.server = ServerSocketChannel.open();
            this.server.configureBlocking(false);
            this.server.bind(sockaddr);

            System.out.format("Server listening at %s\n", sockaddr);

            // open the selector and request to listen for ACCEPTS on the ServerSocket
            this.selector = Selector.open();
            this.server.register(this.selector, SelectionKey.OP_ACCEPT);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public String getServerName()
    {
        // return the server's name
        return this.serverName;
    }

    public User getUserByNick(String nick)
    {
        // search the user list for the user with the requested nick
        for (Entry<SocketChannel, User> e : this.clients.entrySet())
        {
            if (e.getValue().getName().equals(nick))
            {
                return e.getValue();
            }
        }

        return null;
    }

    public Channel getChannel(String name)
    {
        // search the channel list for some channel
        // with the requested name
        for (Channel c : this.channels)
        {
            if (c.getName().equals(name))
            {
                return c;
            }
        }

        return null;
    }

    public Channel createChannel(String name)
    {
        // create a new channel with the given name if it
        // does not exist already

        Channel channel = getChannel(name);

        if (channel == null)
        {
            channel = new Channel(name);
            this.channels.add(channel);
            this.channelUsers.put(name, new ArrayList<>());
        }

        return channel;
    }

    public ArrayList<User> getChannelUsers(String channelName)
    {
        // return list of users of Channel 'channelName'
        return this.channelUsers.get(channelName);
    }

    public void addChannelUser(String channelName, User user)
    {
        // add given user to list of channel users
        ArrayList<User> users = getChannelUsers(channelName);
        users.add(user);
    }

    public void removeChannelUser(String channelName, User user)
    {
        // remove given user from channel users
        ArrayList<User> users = getChannelUsers(channelName);
        users.remove(user);
    }

    public void sendPrivateMessage(String target, String source, String message)
    {
        // send PRIVMSG to given user
        User user = getUserByNick(target);
        user.sendMessage(source, "PRIVMSG", target, message);
    }

    public void removeUser(User user)
    {
        System.out.println("Client from " + user.getRemoteAddress() + " quit");

        // try to close the user's SocketChannel
        try
        {
            user.getSocketChannel().close();
            this.clients.remove(user.getSocketChannel());
        }
        catch (IOException e)
        {
            System.out.println("Something went horribly wrong :(");
            e.printStackTrace();
        }
    }

    public void sendChannelMessage(String channelName, String source, String type, String param, String message, boolean includeSender)
    {
        ArrayList<User> users = getChannelUsers(channelName);

        for (User u : users)
        {
            if (includeSender == false && u.getIdent().equals(source))
            {
                continue;
            }

            u.sendMessage(source, type, param, message);
        }
    }

    private void closeConnection()
    {
        System.out.println("Caught interrupt, shutting server down");

        // close the ServerSocketChannel, it's underlying socket
        // and close the selector if present
        if (this.selector != null)
        {
            try
            {
                this.selector.close();
                this.server.socket().close();
                this.server.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    public void run()
    {
        try
        {
            // loop until we interrupt using a Ctrl+C on terminal
            while (!Thread.currentThread().isInterrupted())
            {
                // get current system microtime
                final long oldTime = System.currentTimeMillis();

                // wait 100msec at most for network activity before switching to next SocketChannel
                this.selector.select(100);

                // retrieve all SocketChannels that have pending actions
                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

                // loop through all of them
                while (keys.hasNext())
                {
                    // get SocketChannel specific settings
                    SelectionKey key = keys.next();
                    keys.remove();

                    // seems to somethinh wrong with our SocketChannel, just skip it
                    if (!key.isValid()){
                        continue;
                    }

                    // there's some client waiting for acceptance
                    if (key.isAcceptable())
                    {
                        // get the client's SocketChannel
                        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
                        SocketChannel socketChannel = serverSocketChannel.accept();

                        System.out.format("Client connected from %s\n", socketChannel.getRemoteAddress());

                        // tell the SocketChannel that we need non-blocking IO and request READ/WRITE permissions
                        socketChannel.configureBlocking(false);
                        socketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);

                        // create a new user and it to the list of users
                        User user = new User(socketChannel);
                        user.setup(this.userCounter);

                        ++this.userCounter;

                        clients.put(socketChannel, user);
                    }

                    if (key.isWritable())
                    {
                        // get SocketChannel
                        SocketChannel socketChannel = (SocketChannel) key.channel();

                        // get the user we are talking to
                        User user = clients.get(socketChannel);

                        // check for pending messages in the outgoing buffer and send them if needed
                        while (user.hasOutgoingMessage())
                        {
                            byte[] message = user.getNextOutgoingMessage().getBytes();
                            socketChannel.write(ByteBuffer.wrap(message));
                        }
                    }

                    if (key.isReadable())
                    {
                        // get SocketChannel
                        SocketChannel socketChannel = (SocketChannel) key.channel();

                        // allocate some buffer
                        ByteBuffer readBuffer = ByteBuffer.allocate(1024);
                        readBuffer.clear();
                        int read;

                        try
                        {
                            // try to get message from user
                            read = socketChannel.read(readBuffer);
                        }
                        catch (IOException e)
                        {
                            System.out.format("Reading problem on connection %s, closing connection\n", socketChannel.getRemoteAddress());
                            key.cancel();
                            socketChannel.close();
                            continue;
                        }

                        // client closed the SocketChannel
                        if (read == -1)
                        {
                            System.out.format("Client disconnected from %s\n", socketChannel.getRemoteAddress());

                            // get the user we were talking to
                            User user = this.clients.get(socketChannel);
                            // default quit
                            user.handleQuit(new ArrayList<>());

                            // close SocketChannel properly
                            socketChannel.close();
                            // cancel our READ/WRITE permissions
                            key.cancel();
                            continue;
                        }

                        // switch buffer to extraction mode
                        readBuffer.flip();

                        // get message from network buffer into something we can use
                        byte[] data = new byte[1024];
                        readBuffer.get(data, 0, read);

                        // get the user we are talking to
                        User user = this.clients.get(socketChannel);

                        // convert from byte array to string
                        String cmdline = new String(data);

                        // clients may send several commands at the same tie so that they
                        // arrive as one message - just split at the newlines and proceed
                        // line by line
                        for (String line : cmdline.split("\n"))
                        {
                            // parse the command and the args out of the line
                            Command cmd = user.parseLine(line);
                            // execute it as the user who send the command
                            user.execute(cmd);
                        }
                    }

                    // get current system microtime
                    final long newTime = System.currentTimeMillis();

                    // if the last loop took less than 0.01sec,
                    // we want to sleep until we reach 0.01sec loop execution time
                    if (newTime - oldTime < 10)
                    {
                        try
                        {
                            Thread.currentThread().sleep(10 - (newTime - oldTime));
                        }
                        catch (InterruptedException e)
                        {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            closeConnection();
        }
    }
}
