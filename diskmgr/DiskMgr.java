package diskmgr;

import global.GlobalConst;
import global.Minibase;
import global.Page;
import global.PageId;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * <h3>Minibase Disk Manager</h3>
 * The disk manager layer manages a database of disk pages.
 * It provides several services based on those pages.
 * <ol>
 * <li>A logical file layer: Read and write each page and store
 * related statistics
 * <li>Allocate and deallocate disk pages.  Store a space map indicating
 * which pages are currently allocated.
 * <li>Groups of pages may be organized as a file, by a higher layer.
 * </ol>
 * The disk manager manages a library of entries consisting of the name of
 * a file and the page number of the first page in the file.
  */
public class DiskMgr implements GlobalConst {

  /** Number of actual bits per page. */
  protected static final int BITS_PER_PAGE = PAGE_SIZE * 8;

  // --------------------------------------------------------------------------

  /** Pages in the database are stored as an Operating System file.  
   * This is the name of that file. */
  protected String name;

  /** Reference to the OS file. */
  protected RandomAccessFile fp;
  
  /** Database size, in pages. */
  protected int num_db_pages;

  /** Number of disk page reads since database construction. */
  protected int read_cnt;

  /** Number of disk page writes since database construction. */
  protected int write_cnt;

  // ------Manage the DB--------------------


  /**
   * Creates and opens a new database with the given OS file name and specified
   * number of pages.
   */
  public void createDB(String fname, int num_db_pgs) {

    // save the parameters locally
    name = fname;
    num_db_pages = (num_db_pgs > 2) ? num_db_pgs : 2;

    // overwrite an existing file
    File DBfile = new File(name);
    DBfile.delete();

    // create the database file, num_pages pages long
    try {
      fp = new RandomAccessFile(fname, "rw");
      fp.seek((long) (num_db_pages * PAGE_SIZE - 1));
      fp.writeByte(0);
    } catch (IOException exc) {
      Minibase.haltSystem(exc);
    }

    // create and initialize the first DB page
    PageId pageId = new PageId(FIRST_PAGEID);
    DBFirstPage firstpg = new DBFirstPage();
    Minibase.BufferManager.pinPage(pageId, firstpg, PIN_MEMCPY);
    firstpg.setNumDBPages(num_db_pages);
    Minibase.BufferManager.unpinPage(pageId, UNPIN_DIRTY);

    // calculate how many pages are needed for the space map; reserve
    // page 0 plus room for the space map
    int num_map_pages = (num_db_pages + BITS_PER_PAGE - 1) / BITS_PER_PAGE;
    set_bits(pageId, 1 + num_map_pages, 1);

  } // public void createDB(String fname, int num_pgs)

  /**
   * Open the database with the given OS file name.
   */
  public void openDB(String fname) {

    // save the name and open the OS file
    name = fname;
    File DBfile = new File(name);
    if (!DBfile.exists())
    	throw new IllegalStateException("File "+name+" does not exist\n");
    try {
      fp = new RandomAccessFile(fname, "rw");
    } catch (IOException exc) {
      Minibase.haltSystem(exc);
    }

    // read the first page
    PageId pageId = new PageId(FIRST_PAGEID);
    Page apage = new Page();
    Minibase.BufferManager.pinPage(pageId, apage, PIN_DISKIO);

    // get the total number of pages
    DBFirstPage firstpg = new DBFirstPage(apage);
    num_db_pages = firstpg.getNumDBPages();
    Minibase.BufferManager.unpinPage(pageId, UNPIN_CLEAN);

  } // public void openDB(String fname)

  /**
   * Close the database file.  Ensure that buffer contents have been
   * written to disk and close the OS file.
   */
  public void closeDB() {
    try {
      Minibase.BufferManager.flushAllFrames();
      fp.close();
    } catch (IOException exc) {
      Minibase.haltSystem(exc);
    }
  }

  /**
   * Destroy the database, removing the file that stores it.
   */
  public void destroyDB() {
    closeDB();
    File DBfile = new File(name);
    DBfile.delete();
  }
  
//-----Manage Logical File Layer -------------------
  /**
   * Reads the contents of the specified page from disk.
   * 
   * @param pageno identifies the page to read.  It is the page number
   * in the OS file.  Also referred to as the Id of the page.
   * @param mempage output parameter to hold the contents of the page
   * @throws IllegalArgumentException if pageno is invalid
   */
  public void read_page(PageId pageno, Page mempage) {

    // validate the page id
    if ((pageno.pid < 0) || (pageno.pid >= num_db_pages)) {
      throw new IllegalArgumentException("Invalid page number; read aborted");
    }

    // seek to the correct page on disk and read it
    try {
      fp.seek((long) (pageno.pid * PAGE_SIZE));
      fp.read(mempage.getData());
      read_cnt++;
    } catch (IOException exc) {
      Minibase.haltSystem(exc);
    }

  } // public void read_page(PageId pageno, Page mempage)

  /**
   * Writes the contents of the given page to disk.
   * 
   * @param pageno identifies the page to write
   * @param mempage holds the contents of the page
   * @throws IllegalArgumentException if pageno is invalid
   */
  public void write_page(PageId pageno, Page mempage) {

    // validate the page id
    if ((pageno.pid < 0) || (pageno.pid >= num_db_pages)) {
      throw new IllegalArgumentException("Invalid page number; write aborted");
    }

    // seek to the correct page on disk and write it
    try {
      fp.seek((long) (pageno.pid * PAGE_SIZE));
      fp.write(mempage.getData());
      write_cnt++;
    } catch (IOException exc) {
      Minibase.haltSystem(exc);
    }

  } // public void write_page(PageId pageno, Page mempage)
  
  /**
   * Gets the number of disk reads since database construction.
   */
  public int getReadCount() {
    return read_cnt;
  }
  /**
   * Gets the number of disk writes since database construction.
   */
  public int getWriteCount() {
    return write_cnt;
  }

//-----Manage allocation and deallocation of pages -------------------
 
  /**
   * Allocates a run of disk pages.
   * run = contiguous sequence of pages
   * 
   * @return The id of the first page in the run
   * @throws IllegalArgumentException if run_size is invalid
   * @throws IllegalStateException if there is no room for a run
   * of that length
   */
  public PageId allocate_page(int run_size) {

    // validate the run size
    if ((run_size < 1) || (run_size > num_db_pages)) {
      throw new IllegalArgumentException("Invalid run size; allocate aborted");
    }

    // calculate the size of the space map
    int num_map_pages = (num_db_pages + BITS_PER_PAGE - 1) / BITS_PER_PAGE;
    int current_run_start = 0;
    int current_run_length = 0;

    // this loop goes over each page in the space map
    PageId pgid = new PageId();
    Page apage = new Page();
    for (int i = 0; i < num_map_pages; ++i) {

      // pin the current space-map page
      pgid.pid = i + 1;
      Minibase.BufferManager.pinPage(pgid, apage, PIN_DISKIO);

      // get the number of bits on current page
      int num_bits_this_page = num_db_pages - i * BITS_PER_PAGE;
      if (num_bits_this_page > BITS_PER_PAGE)
        num_bits_this_page = BITS_PER_PAGE;

      // Walk the page looking for a sequence of 0 bits of the appropriate
      // length. The outer loop steps through the page's bytes, the inner
      // one steps through each byte's bits.
      byte[] pagebuf = apage.getData();
      for (int byteptr = 0; num_bits_this_page > 0
          && current_run_length < run_size; byteptr++) {

        // initialize bit mask
        Byte mask = new Byte(new Integer(1).byteValue());
        byte tmpmask = mask.byteValue();

        // search the page.  If you see a 0, increment the current
        // run.  If you see a 1, restart the current run.
        while (mask.intValue() != 0 && (num_bits_this_page > 0)
            && (current_run_length < run_size)) {

          // if a 1 is found
          if ((pagebuf[byteptr] & tmpmask) != 0) {
            current_run_start += current_run_length + 1;
            current_run_length = 0;
          } else {
            current_run_length++;
          }

          // advance to the next bit
          tmpmask <<= 1;
          mask = new Byte(tmpmask);
          num_bits_this_page--;

        } // while

      } // inner loop

      // unpin the current space-map page
      Minibase.BufferManager.unpinPage(pgid, UNPIN_CLEAN);

    } // outer loop

    // check for disk full exception
    if (current_run_length < run_size) {
      throw new IllegalStateException("Not enough space left; allocate aborted");
    }

    // update the space map and return the resulting page id
    PageId firstpg = new PageId(current_run_start);
    set_bits(firstpg, run_size, 1);
    return firstpg;

  } // public PageId allocate_page(int run_size)

  /**
   * Allocates a single page (i.e. run size 1) on disk.
   * 
   * @return The id of the first page in the run
   * @throws IllegalStateException if the database is full
   */
  public PageId allocate_page() {
    return allocate_page(1);
  }
  
   /**
   * Deallocates a run of pages on disk.
   * The bits in the space map are just set to 0.
   * The space map is not checked to see that their values were 1,
   * i.e. that the pages were previously allocated.
   * 
   * @param firstid id of the first page to deallocate
   * @param run_size number of pages to deallocate
   * @throws IllegalArgumentException if firstid or run_size is invalid
   */
  public void deallocate_page(PageId firstid, int run_size) {

    // validate the page id
    if ((firstid.pid < 0) || (firstid.pid >= num_db_pages)) {
      throw new IllegalArgumentException(
          "Invalid page number; deallocate aborted");
    }

    // validate the run size
    if (run_size < 1) {
      throw new IllegalArgumentException("Invalid run size; deallocate aborted");
    }

    // update the space map
    set_bits(firstid, run_size, 0);

  } // public void deallocate_page(PageId firstid, int run_size)

  /**
   * Deallocates a single page (i.e. run size 1) on disk.
   * 
   * @param pageno identifies the page to deallocate
   * @throws IllegalArgumentException if firstid is invalid
   */
  public void deallocate_page(PageId pageno) {
    deallocate_page(pageno, 1);
  }
  /**
   * Gets the number of allocated disk pages.
   */
  public int getAllocCount() { 

    // initialize reused variables
    int count = 0;
    int bit_number = 0;
    PageId pgid = new PageId();
    Page apage = new Page();

    // iterate each page in the space map
    int num_map_pages = (num_db_pages + BITS_PER_PAGE - 1) / BITS_PER_PAGE;
    for (int i = 0; i < num_map_pages; i++) {

      // pin the space-map page
      pgid.pid = 1 + i;
      Minibase.BufferManager.pinPage(pgid, apage, PIN_DISKIO);

      // how many bits should we examine on this page?
      int num_bits_this_page = num_db_pages - i * BITS_PER_PAGE;
      if (num_bits_this_page > BITS_PER_PAGE) {
        num_bits_this_page = BITS_PER_PAGE;
      }

      // walk the page looking for 1 bits
      byte[] pagebuf = apage.getData();
      for (int pgptr = 0; num_bits_this_page > 0; pgptr++) { // start forloop02
        for (int mask = 1; mask < 256 && num_bits_this_page > 0; mask = (mask << 1), --num_bits_this_page, ++bit_number) {
          int bit = pagebuf[pgptr] & mask;
          if (bit != 0) {
            count++;
          }
        }
      }

      // unpin the space-map page
      Minibase.BufferManager.unpinPage(pgid, UNPIN_CLEAN);

    } // end of forloop01

    // return the resulting count
    return count;

  } // public int getAllocCount()

  /**
   * Print out the database's space map, a bitmap showing which pages are
   * currently allocated.
   */
  public void print_space_map() {

    int num_map_pages = (num_db_pages + BITS_PER_PAGE - 1) / BITS_PER_PAGE;
    int bit_number = 0;

    // this loop goes over each page in the space map
    PageId pgid = new PageId();
    System.out.println("num_map_pages = " + num_map_pages);
    System.out.print("num_pages = " + num_db_pages);
    for (int i = 0; i < num_map_pages; i++) { // start forloop01

      // pin the space-map page
      pgid.pid = 1 + i; // space map starts at page1
      Page apage = new Page();
      Minibase.BufferManager.pinPage(pgid, apage, PIN_DISKIO);

      // how many bits should we examine on this page?
      int num_bits_this_page = num_db_pages - i * BITS_PER_PAGE;
      if (num_bits_this_page > BITS_PER_PAGE) {
        num_bits_this_page = BITS_PER_PAGE;
      }
      System.out.println("\n\nnum_bits_this_page = " + num_bits_this_page
          + "\n");
      if (i > 0)
        System.out.print("\t");

      // Walk the page looking for a sequence of 0 bits of the appropriate
      // length. The outer loop steps through the page's bytes, the inner
      // one steps through each byte's bits.
      int pgptr = 0;
      byte[] pagebuf = apage.getData();
      int mask;
      for (; num_bits_this_page > 0; pgptr++) { // start forloop02

        for (mask = 1; mask < 256 && num_bits_this_page > 0; mask = (mask << 1), --num_bits_this_page, ++bit_number) {
          // start forloop03

          int bit = pagebuf[pgptr] & mask;
          if ((bit_number % 10) == 0) {
            if ((bit_number % 50) == 0) {
              if (bit_number > 0) {
                System.out.println("\n");
              }
              System.out.print("\t" + bit_number + ": ");
            } else {
              System.out.print(' ');
            }
          }

          if (bit != 0) {
            System.out.print("1");
          } else {
            System.out.print("0");
          }

        } // end of forloop03

      } // end of forloop02

      Minibase.BufferManager.unpinPage(pgid, UNPIN_CLEAN);

    } // end of forloop01

    System.out.println();

  } // public void print_space_map()
  
  /**
   * Sets 'run_size' bits in the space map to the given value, starting from
   * 'start_page'.
   */
  protected void set_bits(PageId start_page, int run_size, int value) {

    // locate the run within the space map
    int first_map_page = start_page.pid / BITS_PER_PAGE + 1;
    int last_map_page = (start_page.pid + run_size - 1) / BITS_PER_PAGE + 1;
    int first_bit_no = start_page.pid % BITS_PER_PAGE;

    // the outer loop goes over all space-map pages we need to touch
    for (PageId pgid = new PageId(first_map_page); pgid.pid <= last_map_page; pgid.pid = pgid.pid + 1, first_bit_no = 0) {
      // Start forloop01

      // pin the space-map page
      Page pg = new Page();
      Minibase.BufferManager.pinPage(pgid, pg, PIN_DISKIO);
      byte[] pgbuf = pg.getData();

      // locate the piece of the run that fits on this page
      int first_byte_no = first_bit_no / 8;
      int first_bit_offset = first_bit_no % 8;
      int last_bit_no = first_bit_no + run_size - 1;

      if (last_bit_no >= BITS_PER_PAGE) {
        last_bit_no = BITS_PER_PAGE - 1;
      }

      int last_byte_no = last_bit_no / 8;

      // this loop actually flips the bits on the current page
      int cur_posi = first_byte_no;
      for (; cur_posi <= last_byte_no; ++cur_posi, first_bit_offset = 0) {
        // start forloop02

        int max_bits_this_byte = 8 - first_bit_offset;
        int num_bits_this_byte = (run_size > max_bits_this_byte ? max_bits_this_byte
            : run_size);

        int imask = 1;
        int temp;
        imask = ((imask << num_bits_this_byte) - 1) << first_bit_offset;
        Integer intmask = new Integer(imask);
        Byte mask = new Byte(intmask.byteValue());
        byte bytemask = mask.byteValue();

        if (value == 1) {
          temp = (pgbuf[cur_posi] | bytemask);
          intmask = new Integer(temp);
          pgbuf[cur_posi] = intmask.byteValue();
        } else {
          temp = pgbuf[cur_posi] & (255 ^ bytemask);
          intmask = new Integer(temp);
          pgbuf[cur_posi] = intmask.byteValue();
        }
        run_size -= num_bits_this_byte;

      } // end of forloop02

      // unpin the space-map page
      Minibase.BufferManager.unpinPage(pgid, UNPIN_DIRTY);

    } // end of forloop01

  } // protected void set_bits(PageId start_page, int run_size, int bit)

  //-----Manage File Library-------------------
  
  /**
   * Adds an entry to the file library.  Each entry contains the name of
   * the file and the PageId of the file's first page.
   * The file library is stored on the first page of the database.
   * More pages are allocated as the library grows.
   * 
   * @throws IllegalArgumentException if fname or start_pageno is invalid
   */
  public void add_file_entry(String fname, PageId start_pageno) {

    // validate the arguments
    if (fname.length() > NAME_MAXLEN) {
      throw new IllegalArgumentException("Filename too long; add entry aborted");
    }
    if ((start_pageno.pid < 0) || (start_pageno.pid >= num_db_pages)) {
      throw new IllegalArgumentException(
          "Invalid page number; add entry aborted");
    }

    // does the file already exist?
    if (get_file_entry(fname) != null) {
      throw new IllegalArgumentException(
          "File entry already exists; add entry aborted");
    }

    // search the header pages for the entry slot
    boolean found = false;
    int free_slot = 0;
    DBHeaderPage hpage = new DBHeaderPage();
    PageId hpid = new PageId();
    PageId tmppid = new PageId();
    PageId nexthpid = new PageId(FIRST_PAGEID);
    do {

      // pin the next header page and get its next
      hpid.pid = nexthpid.pid;
      Minibase.BufferManager.pinPage(hpid, hpage, PIN_DISKIO);
      nexthpid = hpage.getNextPage();

      // search the header page for an empty entry
      int entry = 0;
      while (entry < hpage.getNumOfEntries()) {
        hpage.getFileEntry(tmppid, entry);
        if (tmppid.pid == INVALID_PAGEID) {
          break;
        }
        entry++;
      }

      // verify an empty slot was found
      if (entry < hpage.getNumOfEntries()) {
        free_slot = entry;
        found = true;
      } else if (nexthpid.pid != INVALID_PAGEID) {
        // unpin before continuing loop
        Minibase.BufferManager.unpinPage(hpid, UNPIN_CLEAN);
      }

    } while ((nexthpid.pid != INVALID_PAGEID) && (!found));

    // if necessary (and possible), add a new header page to the library
    if (!found) {

      // allocate the new header page
      nexthpid = allocate_page();

      // set the next-page pointer on the previous library page
      hpage.setNextPage(nexthpid);
      Minibase.BufferManager.unpinPage(hpid, UNPIN_DIRTY);

      // pin the newly-allocated directory page
      hpid.pid = nexthpid.pid;
      Minibase.BufferManager.pinPage(hpid, hpage, PIN_MEMCPY);
      hpage.initDefaults();
      free_slot = 0;

    } // if new library page

    // At this point, "hpid" has the page id of the library page with the free
    // slot; "hpage" has the directory_page pointer; "free_slot" is the entry
    // number in the directory where we're going to put the new file entry.
    hpage.setFileEntry(fname, start_pageno, free_slot);
    Minibase.BufferManager.unpinPage(hpid, UNPIN_DIRTY);

  } // public void add_file_entry(String fname, PageId start_pageno)

  /**
   * Deletes a file entry from the file library.
   * 
   * @throws IllegalArgumentException if fname is invalid
   */
  public void delete_file_entry(String fname) {

    // Is the file really in the library?
    if (get_file_entry(fname) == null) {
      throw new IllegalArgumentException(
          "File entry not found in library; delete entry aborted");
    }

    // search the library pages for the entry slot
    boolean found = false;
    int slot = 0;
    DBHeaderPage hpage = new DBHeaderPage();
    PageId hpid = new PageId();
    PageId tmppid = new PageId();
    PageId nexthpid = new PageId(0);
    do {

      // pin the next library page and get its next
      hpid.pid = nexthpid.pid;
      Minibase.BufferManager.pinPage(hpid, hpage, PIN_DISKIO);
      nexthpid = hpage.getNextPage();

      // search the library page for the entry
      int entry = 0;
      String tmpname = null;
      while (entry < hpage.getNumOfEntries()) {
        tmpname = hpage.getFileEntry(tmppid, entry);
        if ((tmppid.pid != INVALID_PAGEID)
            && (tmpname.compareToIgnoreCase(fname) == 0)) {
          break;
        }
        entry++;
      }

      // verify the entry slot was found
      if (entry < hpage.getNumOfEntries()) {
        slot = entry;
        found = true;
      } else {
        // unpin before continuing loop
        Minibase.BufferManager.unpinPage(hpid, UNPIN_CLEAN);
      }

    } while ((nexthpid.pid != INVALID_PAGEID) && (!found));

    // have to delete record at hpnum:slot
    tmppid.pid = INVALID_PAGEID;
    hpage.setFileEntry("\0", tmppid, slot);
    Minibase.BufferManager.unpinPage(hpid, UNPIN_DIRTY);

  } // public void delete_file_entry(String fname)

  /**
   * Looks up the entry for the given file name.
   * 
   * @return PageId of the file's first page, or null if the file
   * is not in the library
   */
  public PageId get_file_entry(String fname) {

    // search the library pages for the entry's slot
    boolean found = false;
    int slot = 0;
    DBHeaderPage hpage = new DBHeaderPage();
    PageId hpid = new PageId();
    PageId tmppid = new PageId();
    PageId nexthpid = new PageId(0);
    do {

      // pin the next library page and get its next
      hpid.pid = nexthpid.pid;
      Minibase.BufferManager.pinPage(hpid, hpage, PIN_DISKIO);
      nexthpid = hpage.getNextPage();

      // search the library page for the entry
      int entry = 0;
      String tmpname;
      while (entry < hpage.getNumOfEntries()) {
        tmpname = hpage.getFileEntry(tmppid, entry);
        if ((tmppid.pid != INVALID_PAGEID)
            && (tmpname.compareToIgnoreCase(fname) == 0)) {
          break;
        }
        entry++;
      }

      // verify the entry slot was found
      if (entry < hpage.getNumOfEntries()) {
        slot = entry;
        found = true;
      }

      // unpin the page before continuing or exiting loop
      Minibase.BufferManager.unpinPage(hpid, UNPIN_CLEAN);

    } while ((nexthpid.pid != INVALID_PAGEID) && (!found));

    // return null if not found
    if (!found) {
      return null;
    }

    // otherwise, return the first page id
    PageId startpid = new PageId();
    hpage.getFileEntry(startpid, slot);
    return startpid;

  } // public PageId get_file_entry(String fname)

} // public class DiskMgr implements GlobalConst
