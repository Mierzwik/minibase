package bufmgr;

import global.GlobalConst;
import global.Minibase;
import global.Page;
import global.PageId;
import diskmgr.*;

import java.util.HashMap;

/**
 * <h3>Minibase Buffer Manager</h3>
 * The buffer manager manages an array of main memory pages.  The array is
 * called the buffer pool, each page is called a frame.
 * It provides the following services:
 * <ol>
 * <li>Pinning and unpinning disk pages to/from frames
 * <li>Allocating and deallocating runs of disk pages and coordinating this with
 * the buffer pool
 * <li>Flushing pages from the buffer pool
 * <li>Getting relevant data
 * </ol>
 * The buffer manager is used by access methods, heap files, and
 * relational operators.
 */
public class BufMgr implements GlobalConst {

    private Page buffer_pool[];
    private FrameDesc frametab[];

    private HashMap<Integer, Integer> page_mapping;

    /**
     * Constructs a buffer manager by initializing member data.
     *
     * @param numframes number of frames in the buffer pool
     */
    public BufMgr(int numframes) {

        buffer_pool = new Page[numframes];
        frametab = new FrameDesc[numframes];
        page_mapping = new HashMap<>();

        for (int i = 0; i < numframes; i++) {
            buffer_pool[i] = new Page();
            frametab[i] = new FrameDesc(i);
        }
    } // public BufMgr(int numframes)

    /**
     * The result of this call is that disk page number pageno should reside in
     * a frame in the buffer pool and have an additional pin assigned to it,
     * and mempage should refer to the contents of that frame. <br><br>
     * <p>
     * If disk page pageno is already in the buffer pool, this simply increments
     * the pin count.  Otherwise, this<br>
     * <pre>
     * 	uses the replacement policy to select a frame to replace
     * 	writes the frame's contents to disk if valid and dirty
     * 	if (contents == PIN_DISKIO)
     * 		read disk page pageno into chosen frame
     * 	else (contents == PIN_MEMCPY)
     * 		copy mempage into chosen frame
     * 	[omitted from the above is maintenance of the frame table and hash map]
     * </pre>
     *
     * @param pageno   identifies the page to pin
     * @param mempage  An output parameter referring to the chosen frame.  If
     *                 contents==PIN_MEMCPY it is also an input parameter which is copied into
     *                 the chosen frame, see the contents parameter.
     * @param contents Describes how the contents of the frame are determined.<br>
     *                 If PIN_DISKIO, read the page from disk into the frame.<br>
     *                 If PIN_MEMCPY, copy mempage into the frame.<br>
     *                 If PIN_NOOP, copy nothing into the frame - the frame contents are irrelevant.<br>
     *                 Note: In the cases of PIN_MEMCPY and PIN_NOOP, disk I/O is avoided.
     * @throws IllegalArgumentException if PIN_MEMCPY and the page is pinned.
     * @throws IllegalStateException    if all pages are pinned (i.e. pool is full)
     */
    public void pinPage(PageId pageno, Page mempage, int contents) {
        
        if (page_mapping.containsKey(pageno.pid)) {
            // If this is already in the buffer pool, just increment pin count
            frametab[page_mapping.get(pageno.pid)].increment_pin_count(); 
        } else {
            // Otherwise we need to find space in our buffer pool for the page depending on what contents is
            switch (contents) {
                case PIN_DISKIO:
                    // Read the page from disk into the frame
                    
                    break;
                case PIN_MEMCPY:
                    // copy mempage into the frame
                    
                    break;
                case PIN_NOOP:
                    // Copy nothing into the frame - the frame contents are irrelevant
                    
                    break;
                default:
                    // content argument contained an invalid value...
                    throw new IllegalArgumentException("contents argument did not contain a valid value");
            }
        }
        

    } // public void pinPage(PageId pageno, Page page, int contents)

    /**
     * Unpins a disk page from the buffer pool, decreasing its pin count.
     *
     * @param pageno identifies the page to unpin
     * @param dirty  UNPIN_DIRTY if the page was modified, UNPIN_CLEAN otherwise
     * @throws IllegalArgumentException if the page is not in the buffer pool
     *                                  or not pinned
     */
    public void unpinPage(PageId pageno, boolean dirty) {

        throw new UnsupportedOperationException("Not implemented");

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
        // I don't think this method is complete yet. 
        PageId newPage = Minibase.DiskManager.allocate_page(run_size);
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
            
            // deallocate the page
            Minibase.DiskManager.deallocate_page(pageno);
            // Remove from page_mapping
            page_mapping.remove(pageno.pid);
            
        }
        

    } // public void freePage(PageId firstid)

    /**
     * Write all valid and dirty frames to disk.
     * Note flushing involves only writing, not unpinning or freeing
     * or the like.
     */
    public void flushAllFrames() {

        throw new UnsupportedOperationException("Not implemented");

    } // public void flushAllFrames()

    /**
     * Write a page in the buffer pool to disk, if dirty.
     *
     * @throws IllegalArgumentException if the page is not in the buffer pool
     */
    public void flushPage(PageId pageno) {

        throw new UnsupportedOperationException("Not implemented");

    }

    /**
     * Gets the total number of buffer frames.
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
