import java.util.ArrayList;

class Command
{
    public String command = "";
    public ArrayList<String> arguments = new ArrayList<>();

    public Command()
    {
        
    }

    public Command(String command, ArrayList<String> arguments)
    {
        this.command = command;
        this.arguments = arguments;
    }
}