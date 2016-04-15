package global;

/**
 * Global constants; implement this interface to access them conveniently.
 */
public interface GlobalConst {

  //
  // Disk Manager Constants
  //

  /** Size of a page, in bytes. 1024 is artificially small 
   * so we can get lots of I/Os with small data files */
  public static final int PAGE_SIZE = 1024;

  /** Page number of an invalid page (i.e. null pointer). */
  public static final int INVALID_PAGEID = -1;

  /** Page number of the first page in a database file. */
  public static final int FIRST_PAGEID = 0;

  /** Maximum size of a name (i.e. of files or attributes). */
  public static final int NAME_MAXLEN = 50;

  //
  // Buffer Manager Constants
  //

  /** Copy the mempage parameter into the frame. */
  public static final int PIN_MEMCPY = 10;

  /** Copy the disk page into the frame. */
  public static final int PIN_DISKIO = 11;

  /** Don't copy anything into the frame. */
  public static final int PIN_NOOP = 12;
  
  /** Forces the page to be written to disk when unpinned. */
  public static final boolean UNPIN_DIRTY = true;

  /** Optimization to avoid writing to disk when unpinned. */
  public static final boolean UNPIN_CLEAN = false;

  //
  // Heap File Constants
  //

  /** Length of an "empty" slot in a heap file page. */
  public static final int EMPTY_SLOT = -1;

  //
  // System Catalog Constants
  //

  /** Maximum length of a column (in bytes). */
  public static final int MAX_COLSIZE = 1001;

  /** Maximum length of a tuple (in bytes). */
  public static final int MAX_TUPSIZE = 1004;

} // public interface GlobalConst
