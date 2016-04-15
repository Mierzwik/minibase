package diskmgr;

import global.Page;
import global.PageId;

/**
 * Header pages contain the space map, the file library, and
 * assorted metadata.
 * The first page of the database is subclassed as DBFirstPage.
 * Other header pages are subclassed as DBDirectoryPage.
 */
class DBHeaderPage extends Page {

  /** Offset of the next page id. */
  protected static final int NEXT_PAGE = 0;

  /** Offset of the number of file entries. */
  protected static final int NUM_OF_ENTRIES = 4;

  /** Offset of the start of file entries. */
  protected static final int START_FILE_ENTRIES = 8;

  /** Size of a file entry (in bytes). */
  protected static final int SIZE_OF_FILE_ENTRY = 4 + NAME_MAXLEN + 2;

  /** Amount of additional bytes used by directory pages. */
  protected static final int DIR_PAGE_USED_BYTES = 8 + 8;

  /** Amount of additional bytes used by the first page. */
  protected static final int FIRST_PAGE_USED_BYTES = DIR_PAGE_USED_BYTES + 4;

  // --------------------------------------------------------------------------

  /**
   * Default constructor; creates a header page with default values.
   */
  public DBHeaderPage() {
    super();
    initDefaults();
  }

  /**
   * Constructor that wraps an existing header page.
   */
  public DBHeaderPage(Page page) {
    super(page.getData());
  }

  /**
   * Initializes the header page with default values.
   */
  protected void initDefaults() {

    // set the next page to invalid
    PageId pageno = new PageId();
    setNextPage(pageno);

    // set the num entries
    int pageusedbytes = DIR_PAGE_USED_BYTES;
    if (this instanceof DBFirstPage) {
      pageusedbytes = FIRST_PAGE_USED_BYTES;
    }
    int num_entries = (PAGE_SIZE - pageusedbytes) / SIZE_OF_FILE_ENTRY;
    setNumOfEntries(num_entries);

    // initialize the page entries
    for (int index = 0; index < num_entries; ++index) {
      int position = START_FILE_ENTRIES + index * SIZE_OF_FILE_ENTRY;
      setIntValue(INVALID_PAGEID, position);
    }

  } // protected void initDefaults(int pageusedbytes)

  /**
   * Gets the next page number.
   */
  public PageId getNextPage() {
    PageId nextPage = new PageId();
    nextPage.pid = getIntValue(NEXT_PAGE);
    return nextPage;
  }

  /**
   * Set the next page number.
   */
  public void setNextPage(PageId pageno) {
    setIntValue(pageno.pid, NEXT_PAGE);
  }

  /**
   * Gets the number of file entries on the page.
   */
  public int getNumOfEntries() {
    return getIntValue(NUM_OF_ENTRIES);
  }

  /**
   * Sets the number of file entries on the page.
   */
  public void setNumOfEntries(int numEntries) {
    setIntValue(numEntries, NUM_OF_ENTRIES);
  }

  /**
   * Gets a file entry name and page id, given the entry number.
   */
  public String getFileEntry(PageId pageNo, int entryNo) {
    int position = START_FILE_ENTRIES + entryNo * SIZE_OF_FILE_ENTRY;
    pageNo.pid = getIntValue(position);
    return getStringValue(position + 4, NAME_MAXLEN + 2);
  }

  /**
   * Sets a file entry name and page id, given the entry number.
   */
  public void setFileEntry(String fname, PageId pageNo, int entryNo) {
    int position = START_FILE_ENTRIES + entryNo * SIZE_OF_FILE_ENTRY;
    setIntValue(pageNo.pid, position);
    setStringValue(fname, position + 4);
  }

} // class DBHeaderPage extends Page
