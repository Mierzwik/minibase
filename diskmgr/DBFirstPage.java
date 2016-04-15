package diskmgr;

import global.Page;

/**
 * First page and starting point of the database file.
 */
class DBFirstPage extends DBHeaderPage {

  /** Offest for the total number of pages. */
  protected static final int NUM_DB_PAGE = PAGE_SIZE - 4;

  // --------------------------------------------------------------------------

  /**
   * Constructs a new first page with default values.
   */
  public DBFirstPage() {
    super();
  }
  
  /**
   * Constructor that wraps an existing first page.
   */
  public DBFirstPage(Page page) {
    super(page);
  }

  /**
   * Sets the number of pages in the DB.
   */
  public void setNumDBPages(int num) {
    setIntValue(num, NUM_DB_PAGE);
  }

  /**
   * Gets the number of pages in the DB.
   */
  public int getNumDBPages() {
    return getIntValue(NUM_DB_PAGE);
  }

} // class DBFirstPage extends DBHeaderPage
