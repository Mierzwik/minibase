package bufmgr;

import global.*;
//import diskmgr.*;

import javax.xml.crypto.dsig.keyinfo.KeyValue;
import java.awt.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Minibase Buffer Manager
 * The buffer manager manages an array of main memory pages.  The array is
 * called the buffer pool, each page is called a frame.
 * It provides the following services:
 * 
 * Pinning and unpinning disk pages to/from frames
 * Allocating and deallocating runs of disk pages and coordinating this with
 * the buffer pool
 * Flushing pages from the buffer pool
 * Getting relevant data
 * 
 * The buffer manager is used by access methods, heap files, and
 * relational operators.
 */
public class BufMgr implements GlobalConst {

    protected Page buffer_pool[];
    protected FrameDesc frametab[];

    // page_mapping will map a PageID.pid to the frametab and buffer_pool index
    protected HashMap<Integer, Integer> page_mapping;
    
    // isFull keeps track of if the buffer is full
    private boolean isFull;
    private Clock replace;
    /**
     * Constructs a buffer manager by initializing member data.
     *
     * @param numframes number of frames in the buffer pool
     */
    public BufMgr(int numframes) {

        buffer_pool = new Page[numframes];
        frametab = new FrameDesc[numframes];
        page_mapping = new HashMap<>();
        isFull = false;
        replace = new Clock(this);
        
        for (int i = 0; i < numframes; i++) {
            buffer_pool[i] = new Page();
            frametab[i] = new FrameDesc();
        }
    } // public BufMgr(int numframes)

    /**
     * The result of this call is that disk page number pageno should reside in
     * a frame in the buffer pool and have an additional pin assigned to it,
     * and mempage should refer to the contents of that frame. 
     *
     * If disk page pageno is already in the buffer pool, this simply increments
     * the pin count.  Otherwise, this
     * 
     * 	uses the replacement policy to select a frame to replace
     * 	writes the frame's contents to disk if valid and dirty
     * 	if (contents == PIN_DISKIO)
     * 		read disk page pageno into chosen frame
     * 	else (contents == PIN_MEMCPY)
     * 		copy mempage into chosen frame
     * 	[omitted from the above is maintenance of the frame table and hash map]
     * 
     *
     * @param pageno   identifies the page to pin
     * @param mempage  An output parameter referring to the chosen frame.  If
     *                 contents==PIN_MEMCPY it is also an input parameter which is copied into
     *                 the chosen frame, see the contents parameter.
     * @param contents Describes how the contents of the frame are determined.
     *                 If PIN_DISKIO, read the page from disk into the frame.
     *                 If PIN_MEMCPY, copy mempage into the frame.
     *                 If PIN_NOOP, copy nothing into the frame - the frame contents are irrelevant.
     *                 Note: In the cases of PIN_MEMCPY and PIN_NOOP, disk I/O is avoided.
     * @throws IllegalArgumentException if PIN_MEMCPY and the page is pinned.
     * @throws IllegalStateException    if all pages are pinned (i.e. pool is full)
     */
    public void pinPage(PageId pageno, Page mempage, int contents) {

        if (page_mapping.containsKey(pageno.pid)) {
            int index = page_mapping.get(pageno.pid);
            mempage.setPage(buffer_pool[index]);
            frametab[index].increment_pin_count(); 
        } else {
            int temp = this.findInvalidFrame();
            int index = -1;
            if (page_mapping.size() == frametab.length) {
                try {
                    index = replace.pickVictim();
                } catch (IllegalStateException exception) {
                    throw exception;
                }
                if (frametab[index].getDirty()) {
                    flushPage(new PageId(index));
                }
            } else {
                index = temp;
            }

            switch (contents) {
                case PIN_DISKIO:
                    // Read the page from disk into the frame
                    Page tempPage = new Page();
                    Minibase.DiskManager.read_page(pageno, tempPage);
                    //
                    // Try removing mempage.copyPage later
                    //

                    removeMappingAndFlush(index);
                    mempage.setPage(tempPage);
                    buffer_pool[index].setPage(tempPage);


                    page_mapping.put(pageno.pid, index);

                    frametab[index].increment_pin_count();
                    frametab[index].setpage_number(pageno.pid); 
                    
                    break;
                case PIN_MEMCPY:
                    // copy mempage into the frame
                    buffer_pool[index].setPage(mempage);

                    removeMappingAndFlush(index);
                    page_mapping.put(pageno.pid, index);

                    frametab[index].increment_pin_count();
                    frametab[index].setpage_number(pageno.pid); 
                    break;
                case PIN_NOOP:
                    // Copy nothing into the frame - the frame contents are irrelevant
                    
                    PageId newPId = new PageId(pageno.pid);

                    removeMappingAndFlush(index);
                    page_mapping.put(newPId.pid, index);

                    frametab[index].increment_pin_count(); 
                    frametab[index].setpage_number(newPId.pid);



                    mempage.setPage(buffer_pool[page_mapping.get(pageno.pid)]);
                    //buffer_pool[page_mapping.get(pageno.pid)].setPage(mempage);
                    //buffer_pool[index].setPage(mempage);
                    break;
                default:
                    // content argument contained an invalid value...
                    throw new IllegalArgumentException("contents argument did not contain a valid value");
            }
        }
        

    } // public void pinPage(PageId pageno, Page page, int contents)

    private void removeMappingAndFlush(int index) {
        int pageToFlush = 0;
        Iterator it = page_mapping.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();

            if (pair.getValue() == index){
                pageToFlush = (Integer)pair.getKey();
                flushPage(new PageId(pageToFlush));
                it.remove();
            }

        }


    }

    /**
     * Find an invalid frame to use
     * 
     * @return invalid frame number, or -1 if invalid
     */
    public int findInvalidFrame() {
        int id = -1;
        for(int i = 0; i < frametab.length; i++) {
            id = frametab[i].getPage_number();
            if (id == -1) {
                return i;
            }
        }
        // No invalid frames were found, buffer is full
        isFull = true;
        return 1;
    } // public int findInvalidFrame()
    
    /**
     * Unpins a disk page from the buffer pool, decreasing its pin count.
     *
     * @param pageno identifies the page to unpin
     * @param dirty  UNPIN_DIRTY if the page was modified, UNPIN_CLEAN otherwise
     * @throws IllegalArgumentException if the page is not in the buffer pool
     *                                  or not pinned
     */
    public void unpinPage(PageId pageno, boolean dirty) {
        if (!page_mapping.containsKey(pageno.pid) || frametab[page_mapping.get(pageno.pid)].getPin_count() == 0 ) {
            throw new IllegalArgumentException("Page is not in the buffer pool or not pinned");
        }
        if (dirty == UNPIN_DIRTY) {
            try {
            this.flushPage(pageno);
            } catch(IllegalArgumentException e) {
                throw e;
            }
        }
        frametab[page_mapping.get(pageno.pid)].decrement_pin_count();
        if (frametab[page_mapping.get(pageno.pid)].getPin_count() == 0) {
            frametab[page_mapping.get(pageno.pid)].setReference_bit(true);
        }
    } // public void unpinPage(PageId pageno, boolean dirty)

    /**
     * Allocates a run of new disk pages and pins the first one in the buffer pool.
     * The pin will be made using PIN_MEMCPY.  Watch out for disk page leaks.
     *
     * @param firstpg  input and output: holds the contents of the first allocated page
     *                 and refers to the frame where it resides
     * @param run_size input: number of pages to allocate
     * @return page id of the first allocated page
     * @throws IllegalArgumentException if firstpg is already pinned
     * @throws IllegalStateException    if all pages are pinned (i.e. pool exceeded)
     */
    public PageId newPage(Page firstpg, int run_size) {
        findInvalidFrame();

        //if (isFull) {
        //    return null;
        //}
        PageId newPage = new PageId();
        newPage = Minibase.DiskManager.allocate_page(run_size);
        pinPage(newPage, firstpg, PIN_DISKIO);
        

        return newPage;
} // public PageId newPage(Page firstpg, int run_size)

    /**
     * Deallocates a single page from disk, freeing it from the pool if needed.
     *
     * @param pageno identifies the page to remove
     * @throws IllegalArgumentException if the page is pinned
     */
    public void freePage(PageId pageno) {
        if (page_mapping.containsKey(pageno.pid)) {
            int index = page_mapping.get(pageno.pid);
            if (frametab[index].getPin_count() > 0) {
                throw new IllegalArgumentException("Page is pinned");
            }

            //buffer_pool[index].setData(new byte[1024]);

            // deallocate the page
            Minibase.DiskManager.deallocate_page(pageno);
            if (page_mapping.containsKey(pageno.pid)) {
                frametab[page_mapping.get(pageno.pid)] = new FrameDesc();
                // Remove from page_mapping
                page_mapping.remove(pageno.pid);

                if (isFull) {
                    isFull = false;
                }

            }
        }
        

    } // public void freePage(PageId firstid)

    /**
     * Write all valid and dirty frames to disk.
     * Note flushing involves only writing, not unpinning or freeing
     * or the like.
     */
    public void flushAllFrames() {
        for (FrameDesc frame : frametab) {
            int pageno = frame.getPage_number();
            if (pageno != -1 && frame.getDirty()) {
                flushPage(new PageId(pageno));
            }
        }
    } // public void flushAllFrames()

    /**
     * Write a page in the buffer pool to disk, if dirty.
     *
     * @throws IllegalArgumentException if the page is not in the buffer pool
     */
    public void flushPage(PageId pageno) {
        // Check if pageno is in the page_mapping
        if (page_mapping.containsKey(pageno.pid)) {

            int index = page_mapping.get(pageno.pid);

            if (pageno.pid == 12) {
                int badValue = Convert.getIntValue(0, buffer_pool[index].getData());
                System.out.print("");
            }

            // Check if page is dirty
            //if (frametab[index].getDirty()) {
            // Write page to disk
            Minibase.DiskManager.write_page(pageno, buffer_pool[index]);
            // Set dirty bit to false
            frametab[index].setDirty(false);
            //}
        } else {
            throw new IllegalArgumentException("pageno is not in the buffer pool");
        }
    }

    /**
     * Gets the total number of buffer frames.
     * 
     * @return returns number of frames
     */
    public int getNumFrames() {
        return frametab.length;
    }

    /**
     * Gets the total number of unpinned buffer frames.
     * 
     * @return Returns the number of unpinned frames
     */
    public int getNumUnpinned() {

        int count = 0;
        for (FrameDesc frame : frametab) {
            if (frame.getPin_count() == 0) {
                count++;
            }
        }
        return count;
    }

} // public class BufMgr implements GlobalConst
