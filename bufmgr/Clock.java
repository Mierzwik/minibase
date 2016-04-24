package bufmgr;


public class Clock {
    // We will probably want to change this, but I just wanted to get it working. 
    BufMgr buf;
    public Clock(BufMgr buf) {
        this.buf = buf;
    }
    
    public int pickVictim() {
        
        for (int counter = 0; counter < buf.frametab.length * 2; counter++) {
            
        }
        return 1;
    }
}
