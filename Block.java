public class Block
{
    public byte[] thisBlock;
    private final int BLOCK_LENGTH = 64;
   
    public Block()
    {
        thisBlock = new byte[BLOCK_LENGTH];
    }
}