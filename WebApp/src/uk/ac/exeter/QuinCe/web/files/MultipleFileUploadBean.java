package uk.ac.exeter.QuinCe.web.files;

import java.util.TreeSet;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.primefaces.model.UploadedFile;

import uk.ac.exeter.QuinCe.data.Files.DataFile;
import uk.ac.exeter.QuinCe.data.Files.DataFileDB;
import uk.ac.exeter.QuinCe.data.Files.FileExistsException;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.ExceptionUtils;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.FileUploadBean;

@ManagedBean(name = "fileUpload")
@ViewScoped
public class MultipleFileUploadBean extends FileUploadBean {
  /**
   * The data file object
   */
  private TreeSet<UploadedDataFile> dataFiles = new TreeSet<UploadedDataFile>();
  private String displayClass = "hidden";

  @Override
  public void processUploadedFile() {
    processUploadedFile(getFile());
  }

  public void processUploadedFile(UploadedFile uploadedFile) {
    UploadedDataFile uploadedDataFile = new PrimeFacesUploadedDataFile(
      uploadedFile);
    dataFiles.add(uploadedDataFile);
    setDisplayClass("");
  }

  public TreeSet<UploadedDataFile> getUploadedFiles() {
    return dataFiles;
  }

  /**
   * Extract files in file list that are not yet extracted
   */
  public void extractNext() {
    for (UploadedDataFile file : dataFiles) {
      if (file.getDataFile() == null && file.isStore()) {
        file.extractFile(getCurrentInstrument(), getAppConfig(), false, false);
        break;
      }
    }
  }

  /**
   * Store selected files. This moves the file(s) to the file store, and updates
   * the database with file info.
   *
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws FileExistsException
   *           If a new file attempts to replace an existing one
   * @throws DatabaseException
   *           If a database error occurs
   * @throws RecordNotFoundException
   *           If a replacement file does not already exist in the database
   */
  public void store() throws MissingParamException, FileExistsException,
    DatabaseException, RecordNotFoundException {
    for (UploadedDataFile file : dataFiles) {
      if (file.isStore() && null != file.getDataFile()) {
        DataFileDB.storeFile(getDataSource(), getAppConfig(),
          file.getDataFile(), file.getReplacementFile());
      }
    }
  }

  /**
   * @return the displayClass
   */
  public String getDisplayClass() {
    return displayClass;
  }

  /**
   * @param displayClass
   *          the displayClass to set
   */
  public void setDisplayClass(String displayClass) {
    this.displayClass = displayClass;
  }

  /**
   * @return the class "hidden" if there are no datafiles yet. Otherwise returns
   *         an empty string.
   */
  public String getStoreFileButtonClass() {
    return dataFiles.size() > 0 ? "" : "hidden";
  }

  /**
   * Called when run types have been updated. This will initiate re-processing
   * of the uploaded files.
   */
  public void updateRunTypes(String fileName) {
    DataFile dataFile = null;

    for (UploadedDataFile file : dataFiles) {
      if (file.getName().equals(fileName)) {
        dataFile = file.getDataFile();
      }
    }

    if (null != dataFile) {
      try {
        InstrumentDB.storeFileRunTypes(getDataSource(),
          dataFile.getFileDefinition().getDatabaseId(),
          dataFile.getMissingRunTypes());
      } catch (Exception e) {
        ExceptionUtils.printStackTrace(e);
      }
    }

    unsetDataFiles();
  }

  private void unsetDataFiles() {
    // Initialize instruments with new run types
    setForceInstrumentReload(true);
    initialiseInstruments();
    TreeSet<UploadedDataFile> tmplist = dataFiles;
    dataFiles = new TreeSet<UploadedDataFile>();
    for (UploadedDataFile file : tmplist) {
      processUploadedFile(
        ((PrimeFacesUploadedDataFile) file).getUploadedFile());
    }
  }
}
