package bufmgr;


public class Clock {
    // We will probably want to change this, but I just wanted to get it working. 
    BufMgr buf;
    public Clock(BufMgr buf) {
        this.buf = buf;
    }
    
    public int pickVictim() {
        
        for (int i = 0; i <= 1; i++) {
            for (FrameDesc frame : buf.frametab) {
                //if (buf.buffer_pool[buf.page_mapping.get(frame.getPage_number())].getData() == null) {
                //    return buf.page_mapping.get(frame.getPage_number());
                //}

                if (frame.getPin_count() == 0) {
                    if (frame.getReference_bit()) {
                        frame.setReference_bit(false); 
                    } else {
                        return buf.page_mapping.get(frame.getPage_number());
                    }
                }
            }
        }
        throw new IllegalStateException();
    }
}
