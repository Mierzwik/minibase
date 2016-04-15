package diskmgr;

import global.Page;

/**
 * Directory pages store file entries and other high-level DB info.
 */
class DBDirectoryPage extends DBHeaderPage {

  /**
   * Constructs a new directory page with default values.
   */
  public DBDirectoryPage() {
    super();
  }
  
  /**
   * Constructor that wraps an existing directory page.
   */
  public DBDirectoryPage(Page page) {
    super(page);
  }

} // class DBDirectoryPage extends DBHeaderPage
