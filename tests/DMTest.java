package tests;

import global.Convert;
import global.Minibase;
import global.Page;
import global.PageId;

/**
 * Test suite for the diskmgr layer.
 */
class DMTest extends TestDriver {

  /** The display name of the test suite. */
  private static final String TEST_NAME = "disk manager tests";

  /** Identifies the first page in the run of pages used in the test. */
  private PageId runStart = new PageId();

  /**
   * Test application entry point; runs all tests.
   */
  public static void main(String argv[]) {

    // create a clean Minibase instance
    DMTest dbt = new DMTest();
    dbt.create_minibase();

    // run all the test cases
    System.out.println("\n" + "Running " + TEST_NAME + "...");
    boolean status = PASS;
    status &= dbt.test1();
    status &= dbt.test2();
    status &= dbt.test3();
    status &= dbt.test4();

    // display the final results
    System.out.println();
    if (status != PASS) {
      System.out.println("Error(s) encountered during " + TEST_NAME + ".");
    } else {
      System.out.println("All " + TEST_NAME + " completed successfully!");
    }

  } // public static void main(String argv[])

  /**
   * 
   */
  protected boolean test1() {

    System.out.print("\n  Test 1 creates a new database and does "
        + "some tests of normal operations:\n");

    boolean status = PASS;

    System.out.print("  - Add some file entries\n");
    PageId pgid = new PageId();

    for (int i = 0; i < 6 && status == PASS; ++i) {
      String name = "file" + i;
      try {
        pgid = Minibase.DiskManager.allocate_page();
      } catch (Exception e) {
        status = FAIL;
        System.err.println("*** Could not allocate a page");
        e.printStackTrace();
      }

      if (status == PASS) {
        try {
          Minibase.DiskManager.add_file_entry(name, pgid);
        } catch (Exception e) {
          status = FAIL;
          System.err.print("*** Could not add file entry " + name + "\n");
          e.printStackTrace();
        }
      }
    }

    if (status == PASS) {
      System.out.print("  - Allocate a run of pages\n");
      try {
        runStart = Minibase.DiskManager.allocate_page(30);
      } catch (Exception e) {
        status = FAIL;
        System.err.println("*** Could not allocate a run of pages");
        e.printStackTrace();
      }
    }

    if (status == PASS) {
      System.out.print("  - Write something on some of them\n");
      for (int i = 0; i < 20 && status == PASS; ++i) {

        String writeStr = "A" + i;

        byte[] data = new byte[PAGE_SIZE]; // leave enough space
        Convert.setStringValue(writeStr, 0, data);
        Page pg = new Page(data);

        try {
          Minibase.DiskManager.write_page(new PageId(runStart.pid + i), pg);
        } catch (Exception e) {
          status = FAIL;
          System.err.print("*** Error writing to page " + (runStart.pid + i)
              + "\n");
          e.printStackTrace();
        }
      }
    }

    if (status == PASS) {
      System.out.print("  - Deallocate some of them\n");
      try {
        Minibase.DiskManager.deallocate_page(new PageId(runStart.pid + 20), 10);
      } catch (Exception e) {
        status = FAIL;
        System.err.print("*** Error deallocating pages\n");
        e.printStackTrace();
      }
    }

    if (status == PASS)
      System.out.print("  Test 1 completed successfully.\n");

    return status;

  } // protected boolean test1()

  /**
   * 
   */
  protected boolean test2() {

    System.out.print("\n  Test 2 opens the database created in "
        + "test 1 and does some further tests:\n");

    boolean status = PASS;

    PageId pgid = new PageId();
    pgid.pid = 0;

    System.out.print("  - Delete some of the file entries\n");
    for (int i = 0; i < 3 && status == PASS; ++i) {
      String name = "file" + i;
      try {
        Minibase.DiskManager.delete_file_entry(name);
      } catch (Exception e) {
        status = FAIL;
        System.err.print("*** Could not delete file entry " + name + "\n");
        e.printStackTrace();
      }
    }

    if (status == PASS) {
      System.out.print("  - Look up file entries that should "
          + "still be there\n");
      for (int i = 3; i < 6 && status == PASS; ++i) {
        String name = "file" + i;
        try {
          pgid = Minibase.DiskManager.get_file_entry(name);
        } catch (Exception e) {
          status = FAIL;
          System.err.print("*** Could not find file entry " + name + "\n");
          e.printStackTrace();
        }
      }
    }

    if (status == PASS) {
      System.out.print("  - Read stuff back from pages we wrote in test 1\n");

      for (int i = 0; i < 20 && status == PASS; ++i) {
        Page pg = new Page();
        try {
          Minibase.DiskManager.read_page(new PageId(runStart.pid + i), pg);
        } catch (Exception e) {
          status = FAIL;
          System.err.print("*** Error reading from page " + (runStart.pid + i)
              + "\n");
          e.printStackTrace();
        }

        String testStr = "A" + i;
        String readStr = new String();
        readStr = Convert.getStringValue(0, pg.getData(), 2 * testStr.length());

        if (readStr.equals(testStr) != true) {
          status = FAIL;
          System.err.print("*** Data read does not match what "
              + "was written on page " + (runStart.pid + i) + "\n");

        }
      }
    }

    // final cleaning up before we leave the test
    try {
      Minibase.DiskManager.deallocate_page(runStart, 26);
    } catch (Exception e) {
      status = FAIL;
      System.err.print("*** Error deallocating pages\n");
      e.printStackTrace();
    }

    if (status == PASS)
      System.out.print("  Test 2 completed successfully.\n");

    return status;

  } // protected boolean test2()

  /**
   * 
   */
  protected boolean test3() {

    System.out.print("\n  Test 3 tests for some error conditions:\n");

    boolean status = PASS;
    PageId pgid = new PageId(0);

    if (status == PASS) {
      System.out.print("  - Look up a deleted file entry\n");
      try {
        pgid = Minibase.DiskManager.get_file_entry("file1");
        if (pgid == null) { // no exception should be thrown in this case
          status = FAIL;
          System.out.println("  --> Failed as expected \n");
        }
      } catch (Exception e) {
        e.printStackTrace();
      }

      if (status == PASS) {
        status = FAIL;
        System.err.println("The expected exception was not thrown\n");

      } else {
        status = PASS;
      }
    }

    if (status == PASS) {
      System.out.print("  - Try to delete a deleted entry again\n");
      try {
        Minibase.DiskManager.delete_file_entry("file1");
      } catch (IllegalArgumentException e) {
        System.out.println("  --> Failed as expected \n");
        status = FAIL;
      } catch (Exception e) {
        e.printStackTrace();
      }

      if (status == PASS) {
        status = FAIL;
        System.err.println("The expected exception was not thrown\n");
      } else {
        status = PASS;
      }
    }

    if (status == PASS) {
      System.out.print("  - Try to delete a nonexistent file entry\n");
      try {
        Minibase.DiskManager.delete_file_entry("blargle");
      } catch (IllegalArgumentException e) {
        System.out.println("  --> Failed as expected \n");
        status = FAIL;
      } catch (Exception e) {
        e.printStackTrace();
      }

      if (status == PASS) {
        status = FAIL;
        System.err.println("The expected exception was not thrown\n");
      } else {
        status = PASS;
      }
    }

    if (status == PASS) {
      System.out.print("  - Look up a nonexistent file entry\n");
      try {
        pgid = Minibase.DiskManager.get_file_entry("blargle");
        if (pgid == null) {
          System.out.println("  --> Failed as expected \n");
          status = FAIL;
        }
      } catch (Exception e) {
        e.printStackTrace();
      }

      if (status == PASS) {
        status = FAIL;
        System.err.println("The expected exception was not thrown\n");
      } else {
        status = PASS;
      }
    }

    if (status == PASS) {
      System.out.print("  - Try to add a file entry that's already there\n");
      try {
        Minibase.DiskManager.add_file_entry("file3", runStart);
      } catch (IllegalArgumentException e) {
        System.out.println("  --> Failed as expected \n");
        status = FAIL;
      } catch (Exception e) {
        e.printStackTrace();
      }

      if (status == PASS) {
        status = FAIL;
        System.err.println("The expected exception was not thrown\n");
      } else {
        status = PASS;
      }
    }

    if (status == PASS) {
      System.out.print("  - Try to add a file entry whose name is too long\n");
      // creat a byte array that is big enough to fail the test
      char[] data = new char[NAME_MAXLEN + 5];
      for (int i = 0; i < NAME_MAXLEN + 5; i++) {
        data[i] = 'x';
      }

      String name = new String(data); // make it big
      try {
        Minibase.DiskManager.add_file_entry(name, new PageId(0));
      } catch (IllegalArgumentException e) {
        System.out.println("  --> Failed as expected \n");
        status = FAIL;
      } catch (Exception e) {
        e.printStackTrace();
      }

      if (status == PASS) {
        status = FAIL;
        System.err.println("The expected exception was not thrown\n");
      } else {
        status = PASS;
      }
    }

    if (status == PASS) {
      System.out.print("  - Try to allocate a run of pages that's too long \n");
      try {
        pgid = new PageId();
        pgid = Minibase.DiskManager.allocate_page(10000);
      } catch (IllegalStateException e) {
        System.out.println("  --> Failed as expected \n");
        status = FAIL;
      } catch (Exception e) {
        e.printStackTrace();
      }

      if (status == PASS) {
        status = FAIL;
        System.err.println("The expected exception was not thrown\n");
      } else {
        status = PASS;
      }
    }

    if (status == PASS) {
      System.out.print("  - Try to allocate a negative run of pages \n");
      try {
        pgid = Minibase.DiskManager.allocate_page(-10); // made up 1 to
        // test -10
      } catch (IllegalArgumentException e) {
        System.out.println("  --> Failed as expected \n");
        status = FAIL;
      } catch (Exception e) {
        e.printStackTrace();
      }

      if (status == PASS) {
        status = FAIL;
        System.err.println("The expected exception was not thrown\n");
      } else {
        status = PASS;
      }
    }

    if (status == PASS) {
      System.out.print("  - Try to deallocate a negative run of pages \n");
      try {
        Minibase.DiskManager.deallocate_page(pgid, -10); // made up 1 to
        // test
        // -10
      } catch (IllegalArgumentException e) {
        System.out.println("  --> Failed as expected \n");
        status = FAIL;
      } catch (Exception e) {
        e.printStackTrace();
      }

      if (status == PASS) {
        status = FAIL;
        System.err.println("The expected exception was not thrown\n");
      } else {
        status = PASS;
      }
    }

    if (status == PASS)
      System.out.print("  Test 3 completed successfully.\n");

    return status;

  } // protected boolean test3()

  /**
   * 
   */
  protected boolean test4() {

    boolean status = PASS;
    int pid = Minibase.DiskManager.getAllocCount();

    System.out.print("\n  Test 4 tests some boundary conditions.\n"
        + "    (These tests are very " + "implementation-specific.)\n");

    PageId pgid = new PageId();
    System.out.print("  - Make sure no pages are pinned\n");
    if (Minibase.BufferManager.getNumUnpinned() != Minibase.BufferManager
        .getNumFrames()) {
      System.err.print("**1* The disk space manager has left "
          + "pages pinned\n");
      status = FAIL;
    }

    // we create a new database that's big enough
    // to require 2 pages to hold its space map
    if (status == PASS) {
      System.out.print("  - Allocate all pages remaining after "
          + "DB overhead is accounted for\n");
      try {
        pgid = Minibase.DiskManager.allocate_page(DB_SIZE - pid);
      } catch (Exception e) {
        status = FAIL;
        e.printStackTrace();
        System.err.print("*** Too little space available: could not "
            + "allocate " + (DB_SIZE - pid) + " pages\n");
      }

      if (status == PASS) {
        if (pgid.pid != pid) {
          status = FAIL;
          System.err.print("*** Expected the first page allocated to "
              + "be page " + pid + "\n");
        } else if (Minibase.BufferManager.getNumUnpinned() != Minibase.BufferManager
            .getNumFrames()) {
          status = FAIL;
          System.err
              .print("*2** The disk space manager has left pages pinned\n");
        } else {
          System.out.print("  - Attempt to allocate one more page\n");
          try {
            pgid = Minibase.DiskManager.allocate_page();
          } catch (IllegalStateException e) {
            status = FAIL;
            System.out.println("  --> Failed as expected \n");
          } catch (Exception e) {
            e.printStackTrace();
          }

          if (status == PASS) {
            status = FAIL;
            System.err.println("The expected exception was not thrown\n");
          } else {
            status = PASS;
          }
        }
      }
    }

    PageId pd = new PageId(pid);
    if (status == PASS) {
      System.out.print("  - Free some of the allocated pages\n");
      try {
        Minibase.DiskManager.deallocate_page(pd, 7);
      }

      catch (Exception e) {
        status = FAIL;
        System.err.println("*** Error deallocating pages\n");
      }
    }

    PageId pd2 = new PageId(pid + 30);
    if (status == PASS) {
      try {
        Minibase.DiskManager.deallocate_page(pd2, 8);
      }

      catch (Exception e) {
        status = FAIL;
        System.err.print("*** Error deallocating pages\n");
      }
    }

    if (status == PASS) {
      System.out.print("  - Allocate some of the just-freed pages\n");
      try {
        pgid = Minibase.DiskManager.allocate_page(8);
      } catch (Exception e) {
        status = FAIL;
        System.err.print("*** Could not allocate pages\n");
      }
    }

    if (status == PASS && pgid.pid != pid + 30) {
      status = FAIL;
      System.err.print("*** Allocated wrong run of pages\n");
    }

    PageId pg11 = new PageId(pid + 8);

    if (status == PASS) {
      System.out.print("  - Free two continued run of the allocated pages\n");
      try {
        Minibase.DiskManager.deallocate_page(pg11, 7);
      } catch (Exception e) {
        status = FAIL;
        System.err.println("*** Error deallocating pages\n");
        e.printStackTrace();
      }

      PageId pg18 = new PageId(pid + 15);
      if (status == PASS) {
        try {
          Minibase.DiskManager.deallocate_page(pg18, 11);
        } catch (Exception e) {
          status = FAIL;
          System.err.print("*** Error deallocating pages\n");
          e.printStackTrace();
        }
      }
    }

    if (status == PASS) {
      System.out.println("  - Allocate back number of pages equal "
          + "to the just freed pages\n");
      try {
        pgid = Minibase.DiskManager.allocate_page(18);
      } catch (Exception e) {
        status = FAIL;
        System.err.print("*** Could not allocate pages\n");
        e.printStackTrace();
      }

      if (status == PASS && pgid.pid != pid + 8) {
        status = FAIL;
        System.err.print("*** Allocated wrong run of pages\n");
      }
    }

    // Delete some leftover file entries
    for (int i = 3; i < 6 && status == PASS; ++i) {
      String name = "file" + i;
      try {
        Minibase.DiskManager.delete_file_entry(name);
      } catch (Exception e) {
        status = FAIL;
        System.err.print("*** Could not delete file entry " + name + "\n");
        e.printStackTrace();
      }
    }

    if (status == PASS) {
      System.out.print("  - Add enough file entries that the directory "
          + "must surpass a page\n");

      // This over-counts, but uses only public info.
      int count = PAGE_SIZE / NAME_MAXLEN + 1;

      for (int i = 0; i < count && status == PASS; ++i) {
        String name = "file" + i;

        // Set every file's first page to be page 0, which doesn't
        // cause an error.
        try {
          Minibase.DiskManager.add_file_entry(name, new PageId(0));
        } catch (Exception e) {
          status = FAIL;
          System.err.print("*** Could not add file " + i + "\n");
          e.printStackTrace();
        }
      }
    }

    if (status == PASS) {

      System.out.print("  - Make sure that the directory has "
          + "taken up an extra page: try to\n"
          + "    allocate more pages than should be available\n");

      // There should only be 6 pages available.
      try {
        pgid = Minibase.DiskManager.allocate_page(7);
      } catch (IllegalStateException e) {
        status = FAIL;
        System.out.println("   --> Failed as expected \n");
      } catch (Exception e) {
        e.printStackTrace();
      } // All other exceptions

      if (status == PASS) {
        status = FAIL;
        System.err.println("The expected exception was not thrown \n");
      } else {
        status = PASS;
      }

      if (status == PASS) {
        try {
          pgid = Minibase.DiskManager.allocate_page(6); // Should work.
        } catch (Exception e) {
          status = FAIL;
          System.err.print("*** But allocating the number that "
              + "should be available failed.\n");
          e.printStackTrace();
        }
      }
    }

    if (status == PASS) {
      System.out.print("  - At this point, all pages should be claimed.  "
          + "Try to allocate one more.\n");
      try {
        pgid = Minibase.DiskManager.allocate_page();
      } catch (IllegalStateException e) {
        status = FAIL;
        System.out.println("   --> Failed as expected \n");
      } catch (Exception e) {
        e.printStackTrace();
      }

      if (status == PASS) {
        status = FAIL;
        System.err.println("The expected exception was not thrown \n");
      } else {
        status = PASS;
      }
    }

    if (status == PASS) {
      System.out.print("  - Free the last two pages: this tests a boundary "
          + "condition in the space map.\n");
      try {
        Minibase.DiskManager.deallocate_page(new PageId(DB_SIZE - 2), 2);
      } catch (Exception e) {
        status = FAIL;
        System.err.print("*** Did not work.\n");
        e.printStackTrace();
      }
      if (status == PASS
          && Minibase.BufferManager.getNumUnpinned() != Minibase.BufferManager
              .getNumFrames()) {
        System.err.print("*** The disk space manager has left pages pinned\n");
        status = FAIL;
      }
    }

    if (status == PASS) {
      System.out.print("  Test 4 completed successfully.\n");
    }

    return status;

  } // protected boolean test4()

} // class DMTest extends TestDriver
