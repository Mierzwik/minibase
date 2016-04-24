package bufmgr;

public class FrameDesc {

    private int page_number;
    private boolean dirty;
    private int pin_count;
    private boolean reference_bit;

    FrameDesc() {
        this.page_number = -1;
        this.dirty = false;
        this.pin_count = 0;
        this.reference_bit = false;
    }
    /**
     * Returns the current page_number
     */
    int getPage_number() {
        return this.page_number;
    }
    
    /**
     * Set the page number
     * 
     * @param pageno 
     */
    void setpage_number(int pageno) {
        this.page_number = pageno;
    }

    /**
     * Sets the dirty bit
     *
     * @param toSet determines what to set the dirty bit to
     */
    void setDirty(boolean toSet) {
        this.dirty = toSet;
    }
    /**
     * Returns whether the dirty bit is true or false
     */
    boolean getDirty() {
        return dirty;
    }

    /**
     * Increments the pin_count by 1
     */
    void increment_pin_count() {
        this.pin_count++;
    }

    /**
     * Decrements the pin_count by 1, as long as the pin_count is > 0
     */
    void decrement_pin_count() {
        if (pin_count > 0) {
            this.pin_count--;
        }
    }

    /**
     * Returns the integer pin_count
     */
    int getPin_count() {
        return this.pin_count;
    }

    /**
     * Sets the boolean reference_bit
     *
     * @param toSet determines what the reference_bit is set to
     */
    void setReference_bit(boolean toSet) {
        this.reference_bit = toSet;
    }

    /**
     * Returns the boolean reference_bit
     */
    boolean getReference_bit() {
        return reference_bit;
    }
}