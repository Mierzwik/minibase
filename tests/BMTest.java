package tests;

import global.Convert;
import global.Minibase;
import global.Page;
import global.PageId;

/**
 * Test suite for the bufmgr layer.
 */
class BMTest extends TestDriver {

  /** The display name of the test suite. */
  private static final String TEST_NAME = "buffer manager tests";

  /**
   * Test application entry point; runs all tests.
   */
  public static void main(String argv[]) {

    // create a clean Minibase instance.  This requires pinning and unpinning
	// the first page of the database.
    BMTest bmt = new BMTest();
    bmt.create_minibase();

    // run all the test cases
    System.out.println("\n" + "Running " + TEST_NAME + "...");
    boolean status = PASS;
    status &= bmt.test1();
    status &= bmt.test2();
    status &= bmt.test3();

    // display the final results
    System.out.println();
    if (status != PASS) {
      System.out.println("Error(s) encountered during " + TEST_NAME + ".");
    } else {
      System.out.println("All " + TEST_NAME + " completed successfully!");
    }

  } // public static void main (String argv[])

  /**
   * 
   */
  protected boolean test1() {

    System.out.print("\n  Test 1 allocates pages, writes and reads them\n");

    // Allocate more pages than there are frames in the buffer pool
    boolean status1 = PASS;
    int toAlloc = Minibase.BufferManager.getNumFrames() + 1;
    Page pg = new Page();  //refers to the current frame
    PageId firstPid = new PageId();

    System.out.print("  - Allocate more pages than there are frames\n");
    try {
      firstPid = Minibase.BufferManager.newPage(pg, toAlloc);
    } catch (Exception e) {
      System.err.print("*** Could not allocate " + toAlloc);
      System.err.print(" new pages in the database.\n");
      e.printStackTrace();
      return false;
    }

    // unpin that first page... to simplify our loop
    try {
      Minibase.BufferManager.unpinPage(firstPid, UNPIN_CLEAN);
    } catch (Exception e) {
      System.err.print("*** Could not unpin the first new page.\n");
      e.printStackTrace();
      status1 = FAIL;
    }

    // Write something on each allocated page.  Pin, write, unpin.
    System.out.print("  - Write something on each one\n");
    PageId pid = new PageId();
    PageId lastPid = new PageId();

    for (pid.pid = firstPid.pid, lastPid.pid = pid.pid + toAlloc; status1 == PASS
        && pid.pid < lastPid.pid; pid.pid = pid.pid + 1) {

      try {
        Minibase.BufferManager.pinPage(pid, pg, PIN_NOOP);
      } catch (Exception e) {
        System.err.print("*** Could not pin new page " + pid.pid + "\n");
        e.printStackTrace();
        return false; //If we can't pin the page, end this test
      }

      // Copy the page number + 99999 onto each page. It seems
      // unlikely that this bit pattern would show up there by
      // coincidence.
      int data = pid.pid + 99999;
      Convert.setIntValue(data, 0, pg.getData());

      try {
        Minibase.BufferManager.unpinPage(pid, UNPIN_DIRTY);
      } catch (Exception e) {
        status1 = FAIL; //record failure but continue the test
        System.err.print("*** Could not unpin dirty page " + pid.pid + "\n");
        e.printStackTrace();
      }
    }

    // Read that something back from each page.  Pin, read, unpin.
    // Because there are more pages than frames, disk I/O will happen here.
    System.out.print("  - Read that something back from each one\n");

    for (pid.pid = firstPid.pid; status1 == PASS && pid.pid < lastPid.pid; 
    		pid.pid = pid.pid + 1) {

      try {
        Minibase.BufferManager.pinPage(pid, pg, PIN_DISKIO);
      } catch (Exception e) {
        System.err.print("*** Could not pin page " + pid.pid + "\n");
        e.printStackTrace();
        return false;  //If we can't pin the page, end this test
      }

      int data = 0;
      data = Convert.getIntValue(0, pg.getData());

      if (status1 == PASS) {
        if (data != (pid.pid) + 99999) {
          status1 = FAIL;  //record failure but continue the test
          System.err.print("*** Read wrong data back from page " + pid.pid + "\n");
        }
      }

      try {
        Minibase.BufferManager.unpinPage(pid, UNPIN_CLEAN);
      } catch (Exception e) {
        status1 = FAIL;  //record failure but continue the test
        System.err.print("*** Could not unpin page " + pid.pid + "\n");
        e.printStackTrace();
      }
    }

    //Free the allocated pages
    if (status1 == PASS) {
      System.out.print("  - Free the allocated pages\n");

      for (pid.pid = firstPid.pid; pid.pid < lastPid.pid; pid.pid = pid.pid + 1) {

        try {
          Minibase.BufferManager.freePage(pid);
        } catch (Exception e) {
          status1 = FAIL;
          System.err.print("*** Error freeing page " + pid.pid + "\n");
          e.printStackTrace();
        }
      }
    }

    if (status1 == PASS)
      System.out.print("  Test 1 completed successfully.\n");

    return status1;

  } // protected boolean test1 ()

  /**
   * 
   */
  protected boolean test2() {

    System.out.print("\n  Test 2 exercises some illegal buffer "
        + "manager operations:\n");

    // tooMany is one more than the number of unpinned frames
    int tooMany = Minibase.BufferManager.getNumUnpinned() + 1;
    Page pg = new Page();
    PageId firstPid = new PageId();
    boolean status2 = PASS;

    System.out.print("  - Try to pin more pages than there are unpinned frames\n");
    
    //Allocate tooMany pages
    try {
      firstPid = Minibase.BufferManager.newPage(pg, tooMany);
    } catch (Exception e) {
      System.err.print("*** Could not allocate " + tooMany);
      System.err.print(" new pages in the database.\n");
      e.printStackTrace();
      return false;
    }

    // pin one less than tooMany pages - this should work
    PageId pid = new PageId();
    PageId lastPid = new PageId();
    for (pid.pid = firstPid.pid + 1, lastPid.pid = firstPid.pid + tooMany - 1; status2 == PASS
        && pid.pid < lastPid.pid; pid.pid = pid.pid + 1) {
      try {
        Minibase.BufferManager.pinPage(pid, pg, PIN_NOOP);
      } catch (Exception e) {
        status2 = FAIL;
        System.err.print("*** Could not pin new page " + pid.pid + "\n");
        e.printStackTrace();
      }
    }

    // make sure the buffer manager thinks all frames are pinned
    if (Minibase.BufferManager.getNumUnpinned() != 0) {
      status2 = FAIL;
      System.err.print("*** The buffer manager thinks it has "
        + Minibase.BufferManager.getNumUnpinned() + " available frames,\n"
        + "    but it should have none.\n");
    }

    // Try to pin the last allocated page.  It should fail.
    if (status2 == PASS) {
      try {
        Minibase.BufferManager.pinPage(lastPid, pg, PIN_NOOP);
        status2 = FAIL;
        System.err.print("Pin too many: The expected exception was not thrown\n");
      } catch (IllegalStateException exc) {
        System.out.println("  --> Failed as expected \n");
      } catch (Exception e) {
        e.printStackTrace();
        System.err.println("  --> Pin too many failed for the wrong reason \n");
        status2 = FAIL;
      }
    }

    //Try to get a second pin on the first allocated page.  It should succeed.
    if (status2 == PASS) {
      try {
        Minibase.BufferManager.pinPage(firstPid, pg, PIN_DISKIO);
      } catch (Exception e) {
        status2 = FAIL;
        System.err.print("*** Could not acquire a second pin on a page\n");
        e.printStackTrace();
      }
    }

    //Try to free the doubly pinned page.  It should fail.
    if (status2 == PASS) {
      System.out.print("  - Try to free a doubly-pinned page\n");
      try {
        Minibase.BufferManager.freePage(firstPid);
        status2 = FAIL;
        System.err.print("Bad free: The expected exception was not thrown\n");
      } catch (IllegalArgumentException e) {
        System.out.println("  --> Failed as expected \n");
      } catch (Exception e) {
        e.printStackTrace();
        System.err.println("  --> Bad free failed for the wrong reason \n");
        status2 = FAIL;
      }
    }

    //Unpin the first page so all pages except the last one have
    // pin count 1.
    if (status2 == PASS) {
      try {
        Minibase.BufferManager.unpinPage(firstPid, UNPIN_CLEAN);
      } catch (Exception e) {
        status2 = FAIL;
        e.printStackTrace();
      }
    }

    // Try again to unpin a page not in the buffer pool.  It should fail because
    // the buffer pool is full.
    if (status2 == PASS) {
      System.out.print("  - Try to unpin a page not in the buffer pool\n");
      try {
        Minibase.BufferManager.unpinPage(lastPid, UNPIN_CLEAN);
        status2 = FAIL;
        System.err.print("Bad unpin: The expected exception was not thrown\n");
      } catch (IllegalArgumentException exc) {
        System.out.println("  --> Failed as expected \n");
      } catch (Exception e) {
        e.printStackTrace();
        System.err.println("  --> Bad unpin failed for the wrong reason \n");
        status2 = FAIL;
      }
    }

    //Be sure all allocated pages are unpinned and free them
    for (pid.pid = firstPid.pid; pid.pid <= lastPid.pid; pid.pid = pid.pid + 1) {
      try {
        if (pid.pid != lastPid.pid)
          Minibase.BufferManager.unpinPage(pid, UNPIN_CLEAN);
        Minibase.BufferManager.freePage(pid);
      } catch (Exception e) {
        status2 = FAIL;
        System.err.print("*** Error freeing or unpinning page " + pid.pid + "\n");
        e.printStackTrace();
      }
    }

    if (status2 == PASS)
      System.out.print("  Test 2 completed successfully.\n");

    return status2;

  } // protected boolean test2 ()

  /**
   * 
   */
  protected boolean test3() {

    System.out.print("\n  Test 3 does more reading and writing\n");

    //runsize is longer than the number of frames, but not twice as long
    int runSize = 2* Minibase.BufferManager.getNumFrames() - 10;
    int curPageIndex;  
    Page pg = new Page();
    PageId pid = new PageId();
    PageId[] pids = new PageId[runSize];
    boolean status3 = PASS;

    System.out.print("  - Allocate, pin and dirty some new pages, one at "
        + "a time, and unpin every other page\n");
    for (curPageIndex = 0; status3 == PASS && curPageIndex < runSize; ++curPageIndex){
      
      //Allocate each page, remember its id
      try {
        pid = Minibase.BufferManager.newPage(pg, 1);
      } catch (Exception e) {
        status3 = FAIL;
        System.err.print("*** Could not allocate new page number " + curPageIndex + 1
            + "\n");
        e.printStackTrace();
        break;
      }
      pids[curPageIndex] = pid;

      // Copy the page number + 99999 onto it. 
      int data = pid.pid + 99999;
      Convert.setIntValue(data, 0, pg.getData());

      // Unpin odd numbered ones.
      if (curPageIndex %2 != 0 ) {
        try {
          Minibase.BufferManager.unpinPage(pid, UNPIN_DIRTY);
        } catch (Exception e) {
          status3 = FAIL;
          System.err.print("*** Could not unpin dirty page with index" + curPageIndex + "\n");
          break;
        }
      }
    }

    if (status3 == PASS) {
      System.out.print("  - Read the written pages and unpin them\n");

      for (curPageIndex = 0; curPageIndex < runSize; ++curPageIndex) {
        pid = pids[curPageIndex];
        
        //Pin the page (even numbered ones are double pinned) and check its value
        try {
          Minibase.BufferManager.pinPage(pid, pg, PIN_DISKIO);
        } catch (Exception e) {
          status3 = FAIL;
          System.err.print("*** Could not pin page " + pid.pid + "\n");
          e.printStackTrace();
          break;
        }
        int data = Convert.getIntValue(0, pg.getData());
        if (data != pid.pid + 99999) {
          status3 = FAIL;
          System.err.print("*** Read wrong data from page " + pid.pid + "\n");
          break;
        }

        //Unpin the page. If it is even, unpin it (dirty) again
        try {
          Minibase.BufferManager.unpinPage(pid, UNPIN_CLEAN);
        } catch (Exception e) {
          status3 = FAIL;
          System.err.print("*** Could not unpin page with index" + curPageIndex + "\n");
          e.printStackTrace();
          break;
        }
        if (curPageIndex % 2 == 0) {
          try {
            Minibase.BufferManager.unpinPage(pid, UNPIN_DIRTY);
          } catch (Exception e) {
            status3 = FAIL;
            System.err.print("*** Could not unpin page with index" + curPageIndex + " again\n");
            e.printStackTrace();
            break;
          }
        }
      }
    }

    //Are all frames unpinned?
    if (Minibase.BufferManager.getNumFrames() != Minibase.BufferManager.getNumUnpinned()){
    	status3 = FAIL;
    	System.err.print("*** Some frames are still pinned\n");
    }
    
    if (status3 == PASS)
      System.out.print("  Test 3 completed successfully.\n");

    return status3;

  } // protected boolean test3 ()

} // class BMTest extends TestDriver
