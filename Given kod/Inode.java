/*
 *  Copyright Mattias Liljeson Oct 12, 2011
 */

/**
 * Class used when working with a block containing an inode. This is a simple 
 * implementation which can be made better with more and better meta data if 
 * wished for. This implementation does not use an index but relies on a linked 
 * list for simplicity
 * 
 * @author Mattias Liljeson <mattiasliljeson.gmail.com>
 */
public class Inode {
    private byte type;
    private int size;
    //private long created;
    //private long lastModified;
    private int dataPtr;

    public Inode(){
        dataPtr = -1;
    }
    
    public Inode(boolean isFolder){
        dataPtr = -1;
        if(isFolder)
            type = 1;
    }
    
    public Inode(byte[] block) {
        load(block);
    }
    
    public boolean load(byte[] block) {
        boolean success = false;
        if(block.length >= FileSystem.BLOCK_SIZE) {
            type = block[0];
            size = FileSystem.byteArrayToInt(block, 1);
            dataPtr = FileSystem.byteArrayToInt(block, 5);
            success = true;
        }
        return success;
    }
    
    public byte[] save() {
        byte[] block = new byte[FileSystem.BLOCK_SIZE];
        block[0] = type;
        FileSystem.intToByteArray(size, block, 1);
        FileSystem.intToByteArray(dataPtr, block, 5);
        return block;
    } 
    
    public int getDataPtr() {
        return dataPtr;
    }

    public void setDataPtr(int dataPtr) {
        this.dataPtr = dataPtr;
    }
    
    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

}
