import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Shell 
{
    Scanner input;
    Scanner currentLine;
    String command;
    FileSystem fs;
    String output;
    int status;
    
    public Shell()
    {
        //input = new Scanner();
        command = new String();
        fs = new FileSystem();
        output = new String();
        status = 0;
    }
    
    
    public void run()
    {
        while(input.hasNext())
        {
        //Get current line
        currentLine = new Scanner(input.nextLine());
        if(!currentLine.hasNext()) command = "next_instructions";
        else command = currentLine.next();
        
        //Process command
        if(command.equals("cr")) //cd <name>
        {
            String name = currentLine.next();
            //create a new file withl the name <name>
            status = fs.create(name);
            
            if(status < 0) output += "error\n"; //Output:error
            else output += "file "+ name +" created\n"; //Output: file <name> created
        }
        else if(command.equals("de")) //de <name>
        {
            String name = currentLine.next();
            //destroy the named file <name>
            status = fs.destroy(name);
            
            if(status < 0) output += "error\n"; //Output:error
            else output += "file "+ name +" destroyed\n"; //Output: file <name> destroyed
        }
        else if(command.equals("op")) //op <name>
        {
            String name = currentLine.next();
            //open the named file <name> for reading and writing; display an index value
            status = fs.open(name);
            
            if(status < 0) output += "error\n"; //Output:error
            else output += "file "+ name +" opened, index=" + status + "\n"; //Output: file <name> opened, index=<index>
        }
        else if(command.equals("cl")) //cl <index>
        {   
            int index = currentLine.nextInt();
            //close the specified file <index>
            status = fs.close(index);
            
            if(status < 0) output += "error\n"; //Output:error
            else output += "file "+ index +" closed\n"; //Output: file <index> closed
        }
        else if(command.equals("rd")) //rd <index> <count>
        {
            int index = currentLine.nextInt();
            int count = currentLine.nextInt();
            
            byte [] bytesRead = new byte[count];
            
            //sequentially read a number of bytes <count> from the specified file <index> and display them on the terminal
            status = fs.read(index, bytesRead, count);
           
            if(status < 0) output += "error\n"; //Output:error
            else //Output: <count> bytes read: <xx...x>
            {
                String stringRepresentation = new String();
                output += index + " read: ";
                try {
                    stringRepresentation = new String(bytesRead, "UTF-8");
                } catch (UnsupportedEncodingException ex) {
                    Logger.getLogger(Shell.class.getName()).log(Level.SEVERE, null, ex);
                }
                output += stringRepresentation + "\n";
            } 
        }
        else if(command.equals("wr")) //wr <index> <char> <count>
        {
            int index = currentLine.nextInt();
            String character = currentLine.next();
            int count = currentLine.nextInt();
            
            //sequentially write <count> number of <char>s into the specified file <index> at its current position
            byte [] myByte = character.getBytes();
            status = fs.write(index, myByte[0], count);
            
            if(status < 0) output += "error\n"; //Output:error
            else output += status + " bytes written\n"; //Output: <count> bytes written
            
        }
        else if(command.equals("sk")) //sk <index> <pos>
        {
            //seek: set the current position of the specified file <index> to <pos>
            int index = currentLine.nextInt();
            int pos = currentLine.nextInt();
            
            status = fs.lseek(index, pos);
            
            if(status < 0) output += "error\n"; //Output:error
            else output += "current position is " + status + "\n"; //Output: current position is <pos>
            
        }
        else if(command.equals("dr")) //dr
        {
            String directoryList;
            //directory: list the names of all file
            directoryList = fs.directory();
            
            if(status < 0) output += "error\n"; //Output:error
            else output += directoryList + "\n"; //Output: <file0> <file1> ... <fileN>       
        }
        else if(command.equals("in")) //in <disk_cont.txt>
        {
            String nameOfDisk = new String();
            if(currentLine.hasNext()) nameOfDisk = currentLine.next();
            else nameOfDisk = "";
            try {
                status = fs.init(nameOfDisk);
            } catch (IOException ex) {
                Logger.getLogger(Shell.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            if(status < 0) output += "error\n"; //Output:error             
            else if(status == 0) output += "disk restored\n"; //If file does not exist, output: disk initialized
            else if(status == 1) output += "disk initialized\n"; //If file does exist, output: disk restored
        }
        else if(command.equals("sv")) //sv <disk_cont.txt>
        {
            //close all files
            for(int i = 0; i < fs.SIZE_OFT; i++)
            {
                fs.close(i);
            }
            
            //save the contents of the disk in the specified file
            status = fs.save(currentLine.next());

            
            if(status < 0) output += "error\n"; //Output:error             
            else if(status == 0) output += "disk saved\n"; //Output: disk saved
        }
        else if(command.equals("next_instructions"))
        {
            output += "\n";
        }
        else //Invalid command
        {
            output += "error\n"; //output: error
        }
        
        try (PrintWriter out = new PrintWriter(new FileWriter("C:\\Users\\arthu_001\\Desktop\\input_output\\output.txt"))) {
            out.print(output);
        }   catch (IOException ex) {
                Logger.getLogger(Shell.class.getName()).log(Level.SEVERE, null, ex);
            }
      }
    }
}
