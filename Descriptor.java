public class Descriptor
{
    public int file_length;
    public int[] blocks;
    private final int FILE_SIZE = 3; //Number of indexes for blocks the descriptor holds
    public final int NUM_OF_DESCRIPTORS = 24;

    public Descriptor()
    {
        file_length = -1; //Indicates descriptor is not associated with any file
        blocks = new int[FILE_SIZE];
        //-1 indicates that the block is not pointing to anything
        blocks[0] = -1;
        blocks[1] = -1;
        blocks[2] = -1;
    }
}
