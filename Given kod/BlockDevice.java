
public abstract class BlockDevice {

    public BlockDevice() {
    }

    public abstract int writeBlock(int p_nBlockNr, byte[] p_abContents);

    public abstract byte[] readBlock(int p_nBlockNr);
}
