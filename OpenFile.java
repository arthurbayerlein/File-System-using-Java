public class OpenFile 
{
    public Block buffer;
    public int descriptorIndex; //Index of the descriptor for 
    public int currentPosition; //Indicates current byte position in buffer
    public int currentBlock; //Indicates block being read on file descriptor
    public int file_length;
    
    public OpenFile()
    {
        buffer = new Block();
        descriptorIndex = -1; //Indicates no file opened
        currentPosition = -1;
    }
}
