import java.util.BitSet;

public class Bitmap 
{
    private final int NUM_OF_BLOCKS = 64; 
    public BitSet map;
    
    public Bitmap()
    {
        map = new BitSet(NUM_OF_BLOCKS);
        map.clear(); //Set all to unused
        
        //Set bitmap block, descriptor blocks and directory file blocks to be set
        map.set(0, 10);
        
    }
    
    void set_bit(int i) //Sets bit at position i
    {
        map.set(i);
    }
    
    void clear_bit(int i) //Unsets bit at position i
    {
        map.clear(i);
    }
    
    int get_free_index() //Returns an index where the bit is free, returns -1 if there is no block available
    {
        return map.nextClearBit(0);
    }
    
    void load_bitmap(Block bitmapBlock) //Load a block into this bitmap structure
    {
        for(int i = 0; i < NUM_OF_BLOCKS; i++)
        {
            if(bitmapBlock.thisBlock[i] > 0) map.set(i);
            else map.clear(i);
        }
    }
    
    void get_bitmap(byte [] memory)
    {
        for(int i = 0; i < NUM_OF_BLOCKS; i++)
        {
            if(map.get(i)) memory[i] = 1;
            else memory[i] = 0;
        }
    }
}
