public class Main
{
    public static void main(String[] args)
    {
        String address = "localhost";
        int port = 6667;

        if (args.length >= 1)
        {
            address = args[0];
        }

        if (args.length >= 2)
        {
            port = Integer.parseInt(args[1]);
        }

        // create server with default or given address and port
        Globals.server = new Server(address, port);

        // run it
        Globals.server.run();
    }
}