import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class FileSystemProject {
    
    public static Shell mainShell = new Shell();
 
    public static void main(String[] args) throws IOException {      
        mainShell.input = new Scanner(new File("C:\\Users\\arthu_001\\Desktop\\input_output\\professor_input.txt"));
        mainShell.run();
        

    }
    
}
