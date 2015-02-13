import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import static java.lang.Math.floor;

public class FileSystem
{
    public final int NUM_OF_BLOCKS = 64;
    public final int BLOCK_SIZE = 64;
    public final int BLOCKS_PER_FILE = 3;
    public final int NUM_OF_DESCRIPTORS = 24;
    public final int DESCRIPTORS_PER_BLOCK = 4;
    public final int SIZE_OFT = 4;
    public final int BITMAP_LOCATION = 0;
    public final int DESCRIPTORS_LOCATION_START = 1;
    public final int DESCRIPTORS_LOCATION_END = 6; //Each block fits 4 descriptors, total of 6
    public final byte EMPTY_BYTE = -50; //Bytes that are unused have value 0  
    
    public IOSystem IO;
    public Bitmap bitmap;
    public Descriptor[] descriptor_table;
    public Directory dir;
    public OpenFile[] oft;
    public PackableMemory intConverter;


    public FileSystem()
    {
        initialize();
    }


    @SuppressWarnings("empty-statement")
    public int create(String symbolic_file_name)
    {
        //Check if the file name exceed the limit of 4 characters
        if(symbolic_file_name.length() > 4) return -16;
        
        //Check if there is a name conflit
        for(int currDirectory = 0; currDirectory < NUM_OF_DESCRIPTORS - 1 && dir.file_indices[currDirectory] != -1; currDirectory++)
        {
            if(symbolic_file_name.equals(dir.file_names[currDirectory]))
            {
                return -15; //Error code indicating name conflict
            }
        }
        //find a free file descriptor
        int freeDescriptor = 0;
        for(freeDescriptor = 0; freeDescriptor < NUM_OF_DESCRIPTORS - 1 && descriptor_table[freeDescriptor].file_length != -1; freeDescriptor++);
        
        if(descriptor_table[freeDescriptor].file_length != -1) //Reached last descriptor and it is occupied -> all descriptors are being used
        {
            return -1; //Return error code
        }
        
        //find a free directory entry
        int freeDirectory;
        for(freeDirectory = 0; freeDirectory < NUM_OF_DESCRIPTORS - 1 && dir.file_indices[freeDirectory] != -1; freeDirectory++);
        
        if(dir.file_indices[freeDirectory] != -1) //Reached last directory and it is being occupied -> no free directories
        {
            return -2; //Return error code
        }
        
            
        //fill both entries
        dir.file_names[freeDirectory] = symbolic_file_name; 
        dir.file_indices[freeDirectory] = freeDescriptor; 
        
        if(!symbolic_file_name.equals("dire")) //Can't change directory if only being created now...
        {
            saveNameDirectoryMemory(freeDirectory, symbolic_file_name);
            saveIndexDirectoryMemory(freeDirectory, freeDescriptor);
        }
        
        descriptor_table[freeDescriptor].file_length = 0;
        
        return 0;
    }


    @SuppressWarnings("empty-statement")
    public int destroy(String symbolic_file_name)
    {

        
        //search directory to find file descriptor
        int file;
        for(file = 0; file < NUM_OF_DESCRIPTORS - 1 && !dir.file_names[file].equals(symbolic_file_name); file++);
        
        if(!dir.file_names[file].equals(symbolic_file_name)) //Reached last directory and name still doesn't match -> file with given name does not exist
        {
            return -3; //Return error code
        }
  
        //Check to see if file being destroyed is not currently opened
        String nameOfCurrFile = new String();
        for(int currOF = 0; currOF < SIZE_OFT; currOF++)
        {
            int indexOfThisFile = oft[currOF].descriptorIndex;
            
            
            for(int i = 0; i < NUM_OF_DESCRIPTORS; i++)
            {
               if(dir.file_indices[i] == indexOfThisFile)    
               {
                   nameOfCurrFile = new String(dir.file_names[i]);
               }
            }
            
            if(symbolic_file_name.equals(nameOfCurrFile))
            {
                return -19; //Error code indicating file being destroyed is open
            }
        }      
        
        //remove directory entry
        dir.file_names[file] = "";
        int descriptorIndex = dir.file_indices[file]; //Save index of descriptor for this file before marking it as unused
        dir.file_indices[file] = -1;
        destroyDirectoryEntryMemory(file);
        
        //update bit map (if file was not empty)
        //free file descriptor
        descriptor_table[descriptorIndex].file_length = -1;
        for(int i = 0; descriptor_table[descriptorIndex].blocks[i] != -1; i++)
        {
            bitmap.clear_bit(descriptor_table[descriptorIndex].blocks[i]);
            descriptor_table[descriptorIndex].blocks[i] = -1;
        }
        
        //return status indicating everything went well
        return 0;
    }


    @SuppressWarnings("empty-statement")
    public int open(String symbolic_file_name)
    {
            
        //search directory to find index of file descriptor (i)
        int file;
        for(file = 0; file < NUM_OF_DESCRIPTORS - 1 && !dir.file_names[file].equals(symbolic_file_name); file++);
     
        if(!dir.file_names[file].equals(symbolic_file_name)) //Reached last directory and name still doesn't match -> file with given name does not exist
        {
            return -4;
        }

        //Check if the file is already open
        for(int i = 0; i < SIZE_OFT; i++)
        {
            if(oft[i].descriptorIndex ==  dir.file_indices[file]) return -30; //Return error
        }
        
        //allocate a free OFT entry (reuse deleted entries)
        int tableIndex;
        for(tableIndex = 0; tableIndex < SIZE_OFT - 1 && oft[tableIndex].descriptorIndex != -1; tableIndex++);

        if(oft[tableIndex].descriptorIndex != -1) //Reached last entry and none is empty -> OFT is full
        {
            return -5;
        }
        
        //fill in current position (0) and file descriptor index (i)
        oft[tableIndex].currentPosition = 0;
        oft[tableIndex].currentBlock = 0;
        oft[tableIndex].descriptorIndex = dir.file_indices[file];
        oft[tableIndex].file_length = descriptor_table[oft[tableIndex].descriptorIndex].file_length;
                
        //Initialize file if nothing has been written to it yet
        if(descriptor_table[oft[tableIndex].descriptorIndex].blocks[0] == -1)
        {
            oft[tableIndex].file_length = 0;
            //Allocate new block (search and update bitmap)
            int freeIndex = bitmap.get_free_index();
            if(freeIndex < 0) //No available new blocks
            {
                return -8; //Return error indicating that no free block is available to initialize this file
            }
            else //Free block found
            {
                bitmap.set_bit(freeIndex); //Mark the index as being allocated
               
                //Update file descriptor with new block number
                descriptor_table[oft[tableIndex].descriptorIndex].blocks[0] = freeIndex;
            }                 
        }
        
        //read block 0 of file into the r/w buffer (read-ahead)
        oft[tableIndex].buffer = IO.ldisk[descriptor_table[oft[tableIndex].descriptorIndex].blocks[0]];

        return tableIndex;
    }
    


    public int close(int index)
    {
        if(oft[index].descriptorIndex == -1) return -23; //OFT already closed
        
        //Write buffer to disk
        IO.ldisk[descriptor_table[oft[index].descriptorIndex].blocks[oft[index].currentBlock]] = oft[index].buffer;
        //Update file length in descriptor
        descriptor_table[oft[index].descriptorIndex].file_length = oft[index].file_length;
         
        //Free OFT entry
        oft[index].descriptorIndex = -1;
        oft[index].currentPosition = 0;
        oft[index].file_length = 0;
        oft[index].buffer = new Block();
        
        //Return status
        return 0; //Everything went well
    }



    public int read(int index, byte[] mem_area, int count)  //Returns number of bytes read
    {
        if(oft[index].descriptorIndex == -1) return -23; //OFT has no file open
        
        OpenFile thisFile = oft[index];
        int numOfBytesRead = 0;

        //Copy from buffer to memory until desired count or end of file is reached
        for(int i = 0; i < count && thisFile.file_length <= BLOCK_SIZE * BLOCKS_PER_FILE; i++)
        {
            if(thisFile.currentPosition == BLOCK_SIZE - 1 && thisFile.currentBlock == 2)
            {
                return 0;
            }
            if(thisFile.currentPosition == BLOCK_SIZE - 1 && thisFile.currentBlock < 2) //Reached end of buffer and not last block
            {
               mem_area[i] = thisFile.buffer.thisBlock[thisFile.currentPosition];
               numOfBytesRead++;
               //write the buffer to disk
                IO.ldisk[descriptor_table[oft[index].descriptorIndex].blocks[oft[index].currentBlock]] = thisFile.buffer;         
                thisFile.currentPosition = 0;
                
               //If nothing is written in this block yet (It hasn't been allocated), stop reading
               if(descriptor_table[oft[index].descriptorIndex].blocks[oft[index].currentBlock] == -1)
               {
                    return numOfBytesRead; 
               }
               
               //Update buffer with next block
               thisFile.buffer = IO.ldisk[descriptor_table[oft[index].descriptorIndex].blocks[oft[index].currentBlock]];
            }
            

            
            //Copy data from buffer into data structure
            mem_area[i] = thisFile.buffer.thisBlock[thisFile.currentPosition];
            numOfBytesRead++;
            
            //update current position
            thisFile.currentPosition++;          
        }
        
        return numOfBytesRead;
    }



    public int write(int index, byte mem_area, int count) //Returns number of bytes written
    {
        if(oft[index].descriptorIndex == -1) return -23; //OFT has no file open
        
        OpenFile thisFile = oft[index]; //Starting from 1, since 0 is always directory
        int numOfBytesWritten = 0;

        //Copy from memory into buffer until desired count or end of file is reached
        for(int i = 0; i < count && thisFile.file_length <= BLOCK_SIZE * BLOCKS_PER_FILE; i++)
        {
            //if block does not exist yet (file is expanding):
            int blockIndex = descriptor_table[thisFile.descriptorIndex].blocks[thisFile.currentBlock];
            if(blockIndex < 0)
            {
                //Allocate new block (search and update bitmap)
                int freeIndex = bitmap.get_free_index();
                if(freeIndex < 0) //No available new blocks
                {
                    return numOfBytesWritten;
                }
                else //Free block found
                {
                    bitmap.set_bit(freeIndex); //Mark the index as being allocated
                   
                    //Update file descriptor with new block number
                    descriptor_table[thisFile.descriptorIndex].blocks[thisFile.currentBlock] = freeIndex;
                }     
            }
            
            if(thisFile.currentPosition > BLOCK_SIZE - 1) //Reached end of buffer
            {
               //write the buffer to disk
               IO.ldisk[descriptor_table[oft[index].descriptorIndex].blocks[oft[index].currentBlock]] = thisFile.buffer;
               
               thisFile.currentBlock += 1;
               
               if(thisFile.currentBlock > 2) //File got full
                {
                    thisFile.currentBlock = 2; //Make it 3 again
                    return numOfBytesWritten;
                }
               
               thisFile.currentPosition = 0; 
               
               //If it is the first time this block is being accessed, initialize it on descriptor
               if(descriptor_table[oft[index].descriptorIndex].blocks[oft[index].currentBlock] == -1)
               {
                    //Allocate new block (search and update bitmap)
                    int freeIndex = bitmap.get_free_index();
                    if(freeIndex < 0) //No available new blocks
                    {
                        return -8; //Return error indicating that no free block is available to initialize this file
                    }
                    else //Free block found
                    {
                        bitmap.set_bit(freeIndex); //Mark the index as being allocated
               
                        //Update file descriptor with new block number
                        descriptor_table[oft[index].descriptorIndex].blocks[oft[index].currentBlock] = freeIndex;
                    }    
               }
               //Update buffer with next block
               thisFile.buffer = IO.ldisk[descriptor_table[oft[index].descriptorIndex].blocks[oft[index].currentBlock]];
            }
            

            
            //Append current character data structure into buffer
            thisFile.buffer.thisBlock[thisFile.currentPosition] = mem_area;
            thisFile.file_length++;
            numOfBytesWritten++;
            
            //update current position
            thisFile.currentPosition++;          
        }
        
        return numOfBytesWritten;
    }



    public int lseek(int index, int pos)
    {
        if(oft[index].descriptorIndex == -1) return -23; //OFT has no file open
        if(pos > (BLOCK_SIZE * BLOCKS_PER_FILE) - 1) return -24; //Cannot seek beyong maximum length
        if(pos > oft[index].file_length) pos = oft[index].file_length;
        
        OpenFile thisFile = oft[index];

        //Compute block position
        int blockForPos = (int)(floor(pos/BLOCK_SIZE));
        
        if(blockForPos > BLOCKS_PER_FILE) //Position out of bounds
        {
            return -6; //Return error code
        }
        
        //If the new position is not within the current block
        if(blockForPos != thisFile.currentBlock)
        {
            //Write the buffer to disk
            IO.ldisk[descriptor_table[thisFile.descriptorIndex].blocks[thisFile.currentBlock]] = oft[index].buffer;
            
            //Set the current position to the new position
            thisFile.currentBlock = blockForPos;
            
            //Read the new block
            oft[index].buffer = IO.ldisk[descriptor_table[thisFile.descriptorIndex].blocks[thisFile.currentBlock]];
        }
        
        //Move to desired position
        thisFile.currentBlock = 0;
        thisFile.currentPosition = 0;
        for(int i = 0; i < pos; i++)
        {
            if(thisFile.currentPosition == BLOCK_SIZE - 1)
            {
                if(thisFile.currentBlock < 2)
                {
                    thisFile.currentBlock++;
                    thisFile.currentPosition = 0;
                }
            }
            else if(thisFile.currentPosition < 63)
            {
                thisFile.currentPosition++;
            }
        }
        
        return pos; //Everything went well, return position
    }



    public String directory() //Return list of files
    {   
        String fileNames = "";
        //for each non-empty entry, print file name (Empty entry has index -1);
        for(int i = 1; i < NUM_OF_DESCRIPTORS; i++)
        {
            if(dir.file_indices[i] >= 0) //Descriptor allocated
            {
                fileNames += dir.file_names[i] + " ";
            }
        }

        return fileNames;
    }



    public int init(String filename) throws IOException //Restore disk from file.txt or create new if no file
    {
        initialize();
        
        
        boolean fileFound = !filename.equals("");
        File file = new File(filename);
        byte fileContent[] = null;
        FileInputStream fin = null;
        
        if(!filename.equals("")){
            try 
            {
                // create FileInputStream object
                fin = new FileInputStream(file);

                fileContent = new byte[(int)file.length()];

                // Reads up to certain bytes of data from this input stream into an array of bytes.
                fin.read(fileContent);
                //create string from byte array
                String s = new String(fileContent);
                System.out.println("File content: " + s);
            }
            catch (FileNotFoundException e) 
            {
                System.out.println("File not found" + e);
                fileFound = false;
            }
            catch (IOException ioe) 
            {
                System.out.println("Exception while reading file " + ioe);
            }
            finally 
            {
                // close the streams using close method
                try 
                {
                    if (fin != null) 
                    {
                        fin.close();
                    }
                }
                catch (IOException ioe) 
                {
                    System.out.println("Error while closing stream: " + ioe);
                }
            }
        }
        

                    
        if(fileFound)
        {
            //Populate ldisk with file content
            int totalCount = 0;
            for(int currBlock = 0; currBlock < NUM_OF_BLOCKS; currBlock++)
            {
                for(int currPosition = 0; currPosition < BLOCK_SIZE; currPosition++, totalCount++)
                {
                    IO.ldisk[currBlock].thisBlock[currPosition] = fileContent[totalCount];
                }
            }    
       
            //Extract bitmap from ldisk and load bitmap in file system
            bitmap.load_bitmap(IO.ldisk[0]);
            bitmap.set_bit(BITMAP_LOCATION);
            
            //Extract descriptors from ldisk and load in descriptor_table
            int currentDescriptorIndex = 0;
            for(int currBlock = DESCRIPTORS_LOCATION_START; currBlock < DESCRIPTORS_LOCATION_END; currBlock++)
            {
                intConverter.mem = IO.ldisk[currBlock].thisBlock;
                
                int currentPosition;
                for(int i = 0; i < DESCRIPTORS_PER_BLOCK; i++, currentDescriptorIndex++)
                {
                    //Reference descriptor which I'm populating
                    Descriptor currentDescriptor = descriptor_table[currentDescriptorIndex];
                    currentPosition = i*16;
                    
                    //Populate descriptor
                    currentDescriptor.file_length = intConverter.unpack(currentPosition);
                    currentDescriptor.blocks[0] = intConverter.unpack(currentPosition+4);
                    currentDescriptor.blocks[1] = intConverter.unpack(currentPosition+8);
                    currentDescriptor.blocks[2] = intConverter.unpack(currentPosition+12);    
                }
                
                //Set bit for this descriptor in bitmap
                bitmap.set_bit(currBlock);
            }
            
            
            //Extract Directory data from ldisk (Blocks 11, 12, 13)
            intConverter.mem = new byte[4];
            int totalLengthDirOFT = 0;
            int currDirEntry = 0;
            for(int currBlock = 10; currBlock < 12; currBlock++)
            {
                for(int currByte = 0; currByte < BLOCK_SIZE; currByte += 8, currDirEntry++) //Every directory entry is 8 bytes
                {
                    if(IO.ldisk[currBlock].thisBlock[currByte] != EMPTY_BYTE) //Check if entry is being used
                    {
                        byte [] nameInBytes = new byte[4];
                        for(int i = 0; IO.ldisk[currBlock].thisBlock[currByte+i] != EMPTY_BYTE && i < 4; i++)
                        {
                            nameInBytes[i] = IO.ldisk[currBlock].thisBlock[currByte+i];
                        }
                        dir.file_names[currDirEntry] = new String(nameInBytes);
                        dir.file_names[currDirEntry] = dir.file_names[currDirEntry].trim(); //Remove empty spaces to conform with previous formatting
                        
                        for(int i = 0; i < 4; i++)
                        {
                            intConverter.mem[i] = IO.ldisk[currBlock].thisBlock[currByte+4+i];
                        }
                        dir.file_indices[currDirEntry] = intConverter.unpack(0);
                    }
                }
            }
            
            open("dire"); //Open directory
            
        }
        else //File not found (It was already created by buffered reader above
        {
            
            //All default values for blocks should be set by the constructors of each data structure
            //Create directory file (It will be saved in first descriptor since it is free);
            create("dire");
            open("dire");
            saveNameDirectoryMemory(0, "dire");
            saveIndexDirectoryMemory(0, 0);
            write(0, EMPTY_BYTE, 192); //Pre-allocate first few blocks for directory so I know where to find it next time (Designer choice)
            oft[0].file_length = 8; //Didn't actually write anything. Was just reserving the blocks
            
        }
        
        if(fileFound) return 0; //Return status indicating that file was found (Disk restored)
        
        return 1; //Return status indicating that file was not found and therefore created (Disk initialized)
     }
    
    public int save(String file)
    {        
        //Put bitmap back into ldisk
        bitmap.get_bitmap(IO.ldisk[0].thisBlock);
 
        close(0); //Close directory file and save the content into disk

        //Put updated descriptors into ldisk
        int currentDescriptorIndex = 0;
        for(int currBlock = DESCRIPTORS_LOCATION_START; currBlock < DESCRIPTORS_LOCATION_END; currBlock++)
        {
            //Store integers into packable memory
            intConverter.mem = new byte[BLOCK_SIZE];
                
            int currentPosition;
            for(int i = 0; i < DESCRIPTORS_PER_BLOCK; i++, currentDescriptorIndex++)
            {
                //Reference descriptor which I'm populating
                Descriptor currentDescriptor = descriptor_table[currentDescriptorIndex];
                currentPosition = i*16;
                    
                //Populate descriptor
                intConverter.pack(currentDescriptor.file_length, currentPosition);
                intConverter.pack(currentDescriptor.blocks[0], currentPosition+4);
                intConverter.pack(currentDescriptor.blocks[1], currentPosition+8);
                intConverter.pack(currentDescriptor.blocks[2], currentPosition+12);  
            }
            
            IO.ldisk[currBlock].thisBlock = intConverter.mem;
        }
        
        //Open file. If it doesn't exist create one
        BufferedOutputStream bs;

        try 
        {
            FileOutputStream fs = new FileOutputStream(new File(file));
            bs = new BufferedOutputStream(fs);
            
            for(int i = 0; i < NUM_OF_BLOCKS; i++)
            {
                bs.write(IO.ldisk[i].thisBlock); //Save ldisk content into file
            }
            
            bs.close();
        } 
        catch (IOException e) 
        {
        }
        
        return 0; //Everything went well
    }
    
    public void initialize()
    {
        IO = new IOSystem();
        
        //Initialize all bytes as unused
        for(int i = 0; i < NUM_OF_BLOCKS; i++)
        {
            for(int j = 0; j < BLOCK_SIZE; j++)
            {
                IO.ldisk[i].thisBlock[j] = EMPTY_BYTE;
            }
        }
        
        bitmap = new Bitmap();
        
        //Create descriptors
        descriptor_table = new Descriptor[NUM_OF_DESCRIPTORS];
        for(int i = 0; i < NUM_OF_DESCRIPTORS; i++)
        {
            descriptor_table[i] = new Descriptor();
        }
        
        dir = new Directory(); //Create directory
        
        //Create open file table
        oft = new OpenFile[SIZE_OFT];
        for(int i = 0; i < SIZE_OFT; i++)
        {
            oft[i] = new OpenFile();
        }
        
        intConverter = new PackableMemory(BLOCK_SIZE); //Create converter for converting integers and bytes
    }
    
    
    //Functions to change directory in the memory (Directly on open file) instead of using data structure I created. Easier to save on disk later (Redundancy)
    public void saveNameDirectoryMemory(int dirIndex, String name)
    {
        //Get name with byte array representation and write it to ldisk 
        byte [] nameInBytes = name.getBytes();
        int currChar;
        lseek(0, dirIndex*8);
        for(currChar = 0; currChar < name.length(); currChar++) 
        {
            write(0, nameInBytes[currChar], 1);
        }
        
        write(0, EMPTY_BYTE, 4 - name.length());
    }
    
    public void saveIndexDirectoryMemory(int dirIndex, int value)
    {
        //Store integers into packable memory to convert from int to 4 bytes
        intConverter.mem = new byte[4];
        intConverter.pack(value, 0);
        
        lseek(0, dirIndex*8 + 4);
        for(int currByte = 0; currByte < 4; currByte++) 
        {
            write(0, intConverter.mem[currByte], 1);
        }
    }
    
    public void destroyDirectoryEntryMemory(int dirIndex)
    {
        lseek(0, dirIndex*8);
        write(0, EMPTY_BYTE, 8); //Replace all with -1 to indicate it is not being used
        oft[0].file_length -= 8;
    }
}