public class Directory 
{
     private final int FILENAME_SIZE = 4; //Number of characters a file can be named with
     private final int MAX_FILE_NUMBER = 24;
     String[] file_names;
     int[] file_indices;
     
     
     public Directory()
     {
         file_names = new String[MAX_FILE_NUMBER]; //All files initialized as empty string
         file_indices = new int[MAX_FILE_NUMBER];
         
         for(int i = 0; i < MAX_FILE_NUMBER; i++)
         {
             file_indices[i] = -1; //Initialize all file indices as -1, indicating that file is unused
             file_names[i] = new String();
         }
     }
}
