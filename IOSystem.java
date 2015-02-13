public class IOSystem
{
    public Block[] ldisk;
    private final int NUM_OF_BLOCKS = 64;
    
    public IOSystem()
    {
        ldisk = new Block[NUM_OF_BLOCKS];
        
        for(int i = 0; i < NUM_OF_BLOCKS; i++)
        {
            ldisk[i] = new Block();
        }
    }

    void read_block(int i, Block p)
    {
        p = ldisk[i];
    }

    void write_block(int i, Block p)
    {
        ldisk[i] = p;
    }

}