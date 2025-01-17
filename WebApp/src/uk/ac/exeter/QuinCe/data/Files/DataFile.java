package uk.ac.exeter.QuinCe.data.Files;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.math.NumberUtils;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinitionException;
import uk.ac.exeter.QuinCe.data.Instrument.MissingRunTypeException;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.DateTimeColumnAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.DateTimeSpecification;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.DateTimeSpecificationException;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.MissingDateTimeException;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.PositionException;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategory;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.ExceptionUtils;
import uk.ac.exeter.QuinCe.utils.HighlightedString;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.StringUtils;

/**
 * Class representing a data file
 *
 * @author Steve Jones
 *
 */
public class DataFile {

  public static final String TIME_OFFSET_PROP = "timeOffset";

  /**
   * The database ID of this file
   */
  private long databaseId = DatabaseUtils.NO_DATABASE_RECORD;

  /**
   * The file format definition
   */
  private FileDefinition fileDefinition;

  /**
   * The file name
   */
  private String filename;

  /**
   * The date/time of the first record in the file
   */
  private LocalDateTime startDate = null;

  /**
   * The date/time of the last record in the file
   */
  private LocalDateTime endDate = null;

  /**
   * The number of records in the file
   */
  private int recordCount = -1;

  /**
   * The file contents
   */
  private List<String> contents;

  /**
   * Messages generated regarding the file
   */
  private TreeSet<DataFileMessage> messages;

  /**
   * Misc properties
   */
  private Properties properties;

  /**
   * Max number of messages. Additional messages will be discarded.
   */
  private static final int MAX_MESSAGE_COUNT = 25;

  /**
   * Row in the file where the error happened not set
   */
  private static final int ROW_NOT_SET = -1;

  /**
   * The date in the file header
   */
  private LocalDateTime headerDate = null;

  /**
   * The location of the file store
   */
  private String fileStore;

  /**
   * Run types in this file not defined in the file definition
   */
  private Set<RunTypeAssignment> missingRunTypes = new HashSet<RunTypeAssignment>();

  public List<RunTypeAssignment> getMissingRunTypes() {
    List<RunTypeAssignment> list = new ArrayList<>(missingRunTypes);
    Collections.sort(list);
    return list;
  }

  /**
   * Create a DataFile with the specified definition and contents
   *
   * @param fileStore
   *          The location of the file store
   * @param fileDefinition
   *          The file format definition
   * @param filename
   *          The file name
   * @param contents
   *          The file contents
   * @throws MissingParamException
   *           If any fields are null
   */
  public DataFile(String fileStore, FileDefinition fileDefinition,
    String filename, List<String> contents) throws MissingParamException {
    MissingParam.checkMissing(fileDefinition, "fileDefinition");
    MissingParam.checkMissing(filename, "fileName");
    MissingParam.checkMissing(contents, "contents");

    this.fileDefinition = fileDefinition;
    this.filename = filename;
    this.contents = contents;
    this.properties = defaultProperties();

    messages = new TreeSet<DataFileMessage>();
    boolean fileOK = false;

    try {
      fileOK = extractHeaderDate();
    } catch (Exception e) {
      // Since we were provided with the file contents,
      // we won't be loading them so we can't get an exception here.
    }

    if (fileOK) {
      try {
        validate();
      } catch (Exception e) {
        // Since we were provided with the file contents,
        // we won't be loading them so we can't get an exception here.
      }
    }
  }

  /**
   * Constructor for the basic details of a file. The contents will be loaded on
   * demand if required.
   *
   * @param fileStore
   *          The file store location
   * @param id
   *          The file's database ID
   * @param fileDefinition
   *          The file definition for the file
   * @param filename
   *          The filename
   * @param startDate
   *          The date/time of the first record in the file
   * @param endDate
   *          The date/time of the last record in the file
   * @param recordCount
   *          The number of records in the file
   */
  public DataFile(String fileStore, long id, FileDefinition fileDefinition,
    String filename, LocalDateTime startDate, LocalDateTime endDate,
    int recordCount, Properties properties) {
    this.fileStore = fileStore;
    this.databaseId = id;
    this.fileDefinition = fileDefinition;
    this.filename = filename;
    this.startDate = startDate;
    this.endDate = endDate;
    this.recordCount = recordCount;
    this.properties = properties;
  }

  private Properties defaultProperties() {
    Properties result = new Properties();
    result.setProperty(TIME_OFFSET_PROP, "0");
    return result;
  }

  /**
   * Get the file format description
   *
   * @return The file format description
   */
  public String getFileDescription() {
    return fileDefinition.getFileDescription();
  }

  /**
   * Get the file name
   *
   * @return The file name
   */
  public String getFilename() {
    return filename;
  }

  /**
   * Set a new file definition for the file. The new definition must match the
   * layout of the previous definition, since the file's format must conform to
   * both the new and old definitions.
   *
   * @param newDefinition
   *          The new file definition
   * @throws FileDefinitionException
   *           The the definition does not match the file layout
   */
  public void setFileDefinition(FileDefinition newDefinition)
    throws FileDefinitionException {
    if (!fileDefinition.matchesLayout(newDefinition)) {
      throw new FileDefinitionException(
        "File Definition does not match file contents");
    } else {
      this.fileDefinition = newDefinition;
    }
  }

  /**
   * Get the zero-based row number of the first data row in a file
   *
   * @return The first data row number
   * @throws DataFileException
   *           If the end of the file is reached without finding the end of the
   *           header
   */
  public int getFirstDataLine() throws DataFileException {
    loadContents();
    return fileDefinition.getHeaderLength(contents)
      + fileDefinition.getColumnHeaderRows();
  }

  /**
   * Get the number of rows in the file
   *
   * @return The row count
   */
  public int getContentLineCount() {
    return contents.size();
  }

  /**
   * Get the number of records in the file
   *
   * @return The record count
   * @throws DataFileException
   *           If the record count cannot be calculated
   */
  public int getRecordCount() throws DataFileException {
    if (-1 == recordCount) {
      loadContents();
      recordCount = getContentLineCount() - getFirstDataLine();
    }

    return recordCount;
  }

  /**
   * Get the data from a specified row in the file as a list of string fields.
   * This is the row position in the whole file, including headers.
   *
   * @param row
   *          The row to be retrieved
   * @return The row fields
   * @throws DataFileException
   *           If the requested row is outside the bounds of the file
   */
  public List<String> getRowFields(int row) throws DataFileException {
    List<String> result;

    if (row < getFirstDataLine()) {
      throw new DataFileException(databaseId, row,
        "Requested row is in the file header");
    } else if (row > (getContentLineCount() - 1)) {
      throw new DataFileException(databaseId, row,
        "Requested row is in the file header");
    } else {
      result = StringUtils.trimList(
        Arrays.asList(contents.get(row).split(fileDefinition.getSeparator())));
    }

    return result;
  }

  /**
   * Validate the file contents. Creates a set of {@code DataFileMessage}
   * objects, which can be retrieved using {@code getMessages()}.
   *
   * @throws DataFileException
   *           If the file contents could not be loaded
   */
  public void validate() throws DataFileException {

    loadContents();

    // Check that there is actually data in the file
    int firstDataLine = -1;
    try {
      firstDataLine = getFirstDataLine();
    } catch (DataFileException e) {
      addMessage("File does not contain any data");
    }

    if (firstDataLine > -1) {

      // For each line in the file, check that:
      // (a) The date/time are present and monotonic in the file
      // (b) Has the correct number of columns (for Run Types that aren't
      // IGNORED)
      // (c) The Run Type is recognised

      LocalDateTime lastDateTime = null;
      for (int lineNumber = firstDataLine; lineNumber < getContentLineCount(); lineNumber++) {
        String line = contents.get(lineNumber);

        try {
          LocalDateTime dateTime = fileDefinition.getDateTimeSpecification()
            .getDateTime(headerDate, fileDefinition.extractFields(line));
          if (null != lastDateTime) {
            if (dateTime.compareTo(lastDateTime) <= 0) {
              addMessage(lineNumber, "Date/Time is not monotonic");
            }
          }
        } catch (MissingDateTimeException e) {
          // We don't mind this while scanning the file
        } catch (DateTimeSpecificationException e) {
          addMessage(lineNumber, e.getMessage());
        }

        boolean checkColumnCount = true;

        if (fileDefinition.hasRunTypes()) {
          try {
            RunTypeCategory runType = fileDefinition.getRunTypeCategory(line);
            if (runType.equals(RunTypeCategory.IGNORED)) {
              checkColumnCount = false;
            }
          } catch (FileDefinitionException e) {
            addMessage(lineNumber, e.getMessage());
            if (e instanceof MissingRunTypeException) {
              missingRunTypes.add(((MissingRunTypeException) e).getRunType());
            }
          }
        }

        if (checkColumnCount && fileDefinition.extractFields(line)
          .size() != fileDefinition.getColumnCount()) {
          addMessage(lineNumber, "Incorrect number of columns");
        }
      }
    }
  }

  /*
   * Shortcut method for adding a message to the message list. When the list
   * size reaches MAX_MESSAGE_COUNT messages, a final message saying "Too many
   * messages..." is added, then no more messages are allowed.
   *
   * @param lineNumber The line number. Line number < 0 means no line number.
   *
   * @param message The message text
   */
  private void addMessage(int lineNumber, String message) {
    if (messages.size() == MAX_MESSAGE_COUNT - 1) {
      messages.add(new DataFileMessage("Too many messages..."));
    } else if (messages.size() < MAX_MESSAGE_COUNT - 1) {
      if (lineNumber < 0) {
        messages.add(new DataFileMessage(message));
      } else {
        messages.add(new DataFileMessage(lineNumber, message));
      }
    }
  }

  /**
   * Shortcut method for adding a message to the message list
   *
   * @param message
   *          The message text
   */
  private void addMessage(String message) {
    addMessage(ROW_NOT_SET, message);
  }

  /**
   * Get the messages generated for this file
   *
   * @return The messages
   */
  public TreeSet<DataFileMessage> getMessages() {
    return messages;
  }

  /**
   * Get the number of messages that have been generated for this file
   *
   * @return The message count
   */
  public int getMessageCount() {
    return messages.size();
  }

  /**
   * Get the start date from the file header. This is only applicable if the
   * date format is {@link DateTimeSpecification#HOURS_FROM_START}.
   *
   * @return {@code true} if the header date is successfully extracted;
   *         {@code false} if the date cannot be extracted
   * @throws DataFileException
   *           If the file contents could not be loaded
   */
  private boolean extractHeaderDate() throws DataFileException {

    loadContents();

    boolean result = true;

    DateTimeSpecification dateTimeSpec = fileDefinition
      .getDateTimeSpecification();
    if (dateTimeSpec.isAssigned(DateTimeSpecification.HOURS_FROM_START)) {
      try {
        DateTimeColumnAssignment assignment = dateTimeSpec
          .getAssignment(DateTimeSpecification.HOURS_FROM_START);
        HighlightedString matchedLine = fileDefinition.getHeaderLine(contents,
          assignment.getPrefix(), assignment.getSuffix());
        headerDate = LocalDateTime.parse(matchedLine.getHighlightedPortion(),
          assignment.getFormatter());
      } catch (Exception e) {
        ExceptionUtils.printStackTrace(e);
        addMessage(
          "Could not extract file start date from header: " + e.getMessage());
        result = false;
      }
    }

    return result;
  }

  /**
   * Get the time of the first record in the file. Time offset will not be
   * applied. Lines with invalid/missing dates are ignored.
   *
   * @return The date, or null if the date cannot be retrieved
   */
  public LocalDateTime getRawStartTime() {
    if (null == startDate) {
      try {
        loadContents();
        LocalDateTime foundDate = null;
        int searchLine = getFirstDataLine() - 1;
        int lastLine = getContentLineCount() - 1;

        while (null == foundDate && searchLine <= lastLine) {
          searchLine++;

          try {
            foundDate = getRawTime(searchLine);
          } catch (Exception e) {
            // Ignore errors and try the next line
          }
        }

        if (null != foundDate) {
          startDate = foundDate;
        } else {
          addMessage("No valid dates in file");
        }

      } catch (DataFileException e) {
        addMessage("Unable to extract data from file");
      }
    }

    return startDate;
  }

  /**
   * Get the time of the last record in the file. Time offset will not be
   * applied.
   *
   * @return The date, or null if the date cannot be retrieved
   * @throws DataFileException
   *           If the file contents could not be loaded
   */
  public LocalDateTime getRawEndTime() {
    if (null == endDate) {
      try {
        loadContents();
        LocalDateTime foundDate = null;
        int firstLine = getFirstDataLine();
        int searchLine = getContentLineCount();

        while (null == foundDate && searchLine >= firstLine) {
          searchLine--;

          try {
            foundDate = getRawTime(searchLine);
          } catch (Exception e) {
            // Ignore errors and try the next line
          }
        }

        if (null != foundDate) {
          endDate = foundDate;
        } else {
          addMessage("No valid dates in file");
        }

      } catch (DataFileException e) {
        addMessage("Unable to extract data from file");
      }
    }

    return endDate;
  }

  public LocalDateTime getOffsetStartTime() {
    return applyTimeOffset(getRawStartTime());
  }

  public LocalDateTime getOffsetEndTime() {
    return applyTimeOffset(getRawEndTime());
  }

  public LocalDateTime getStartTime(boolean applyOffset) {
    return applyOffset ? getOffsetStartTime() : getRawStartTime();
  }

  public LocalDateTime getEndTime(boolean applyOffset) {
    return applyOffset ? getOffsetEndTime() : getRawEndTime();
  }

  /**
   * Get the time of a line in the file, without the define offset applied
   *
   * @param line
   *          The line
   * @return The time
   * @throws DataFileException
   *           If any date/time fields are empty
   * @throws MissingDateTimeException
   */
  public LocalDateTime getRawTime(int line) throws DataFileException,
    DateTimeSpecificationException, MissingDateTimeException {
    loadContents();
    return getRawTime(fileDefinition.extractFields(contents.get(line)));
  }

  public LocalDateTime getOffsetTime(List<String> line)
    throws DataFileException, DateTimeSpecificationException,
    MissingDateTimeException {

    return applyTimeOffset(getRawTime(line));
  }

  public LocalDateTime getRawTime(List<String> line)
    throws DateTimeSpecificationException, MissingDateTimeException {

    return fileDefinition.getDateTimeSpecification().getDateTime(headerDate,
      line);
  }

  /**
   * Get the run type for a given line. Returns {@code null} if this file does
   * not contain run types
   *
   * @param line
   *          The line
   * @return The run type for the line
   * @throws DataFileException
   *           If the data cannot be extracted
   * @throws FileDefinitionException
   *           If the run types are invalid
   */
  public String getRunType(int line)
    throws DataFileException, FileDefinitionException {
    String runType = null;

    if (fileDefinition.hasRunTypes()) {
      loadContents();
      runType = fileDefinition.getRunType(contents.get(line), true)
        .getRunName();
    }

    return runType;
  }

  /**
   * Get the run type for a given line. Returns {@code null} if this file does
   * not contain run types
   *
   * @param line
   *          The line
   * @return The run type for the line
   * @throws DataFileException
   *           If the data cannot be extracted
   * @throws FileDefinitionException
   *           If the run types are invalid
   */
  public RunTypeCategory getRunTypeCategory(int line)
    throws DataFileException, FileDefinitionException {
    RunTypeCategory runType = null;

    if (fileDefinition.hasRunTypes()) {
      loadContents();
      runType = fileDefinition.getRunTypeCategory(contents.get(line));
    }

    return runType;
  }

  public String getLongitude(List<String> line) throws PositionException {
    return fileDefinition.getLongitudeSpecification().getValue(line);
  }

  public String getLatitude(List<String> line) throws PositionException {
    return fileDefinition.getLatitudeSpecification().getValue(line);
  }

  /**
   * Get a {@link Double} value from a field.
   * <p>
   * Returns {@code null} if the field string is empty, or the field equals the
   * supplied {@code missingValue} (if it is supplied).
   * </p>
   *
   * @param field
   *          The field
   * @param missingValue
   *          The 'missing' value for the field
   * @return The numeric field value
   * @throws ValueNotNumericException
   *           If the field value is not numeric
   */
  public static Double extractDoubleFieldValue(String field,
    String missingValue) throws ValueNotNumericException {
    Double result = null;

    if (null != field && field.trim().length() > 0) {
      if (null == missingValue || !field.equals(missingValue)) {
        try {
          result = Double.parseDouble(field.replace(",", ""));
        } catch (NumberFormatException e) {
          throw new ValueNotNumericException();
        }
      }
    }

    return result;
  }

  /**
   * Get a {@link Integer} value from a field.
   * <p>
   * Returns {@code null} if the field string is empty, or the field equals the
   * supplied {@code missingValue} (if it is supplied).
   * </p>
   *
   * @param field
   *          The field
   * @param missingValue
   *          The 'missing' value for the field
   * @return The numeric field value
   * @throws ValueNotNumericException
   *           If the field value is not numeric
   */
  public static Integer extractIntFieldValue(String field, String missingValue)
    throws ValueNotNumericException {
    Integer result = null;

    if (null != field && field.trim().length() > 0) {
      if (null == missingValue || !field.equals(missingValue)) {
        try {
          result = Integer.parseInt(field);
        } catch (NumberFormatException e) {
          throw new ValueNotNumericException();
        }
      }
    }

    return result;
  }

  /**
   * Get a {@link String} value from a field.
   * <p>
   * If the field is empty, or equals the supplied {@code missingValue},
   * {@code null} is returned.
   * </p>
   *
   * @param field
   *          The field
   * @param missingValue
   *          The 'missing' value for the field
   * @return The field value
   */
  public static String extractStringFieldValue(String field,
    String missingValue) {
    String result = field;

    if (null != field) {
      result = field.trim();

      if (null != missingValue && !field.equals(missingValue)) {
        result = null;
      }
    }

    return result;
  }

  /**
   * Get the file's database ID
   *
   * @return The database ID
   */
  public long getDatabaseId() {
    return databaseId;
  }

  /**
   * Set the file's database ID
   *
   * @param databaseId
   *          The ID
   */
  protected void setDatabaseId(long databaseId) {
    this.databaseId = databaseId;
  }

  /**
   * Get the file definition object
   *
   * @return The file definition
   */
  public FileDefinition getFileDefinition() {
    return fileDefinition;
  }

  /**
   * Get the contents of the file as a single string
   *
   * @return The file contents
   * @throws DataFileException
   *           If the file contents cannot be retrieved
   */
  public String getContents() throws DataFileException {
    loadContents();

    StringBuilder result = new StringBuilder();

    for (int i = 0; i < getContentLineCount(); i++) {
      result.append(contents.get(i));

      if (i < getContentLineCount() - 1) {
        result.append('\n');
      }
    }

    return result.toString();
  }

  /**
   * Set the contents of the data file
   *
   * @param contents
   *          The contents
   */
  protected void setContents(String contents) {
    this.contents = new ArrayList<String>(
      Arrays.asList(contents.split("[\\r\\n]+")));

    StringUtils.removeBlankTailLines(this.contents);
  }

  /**
   * Get the raw bytes for a file
   *
   * @return The file
   * @throws IOException
   *           If the file cannot be read
   */
  public byte[] getBytes() throws IOException {
    return FileStore.getBytes(fileStore, this);
  }

  /**
   * Load the contents of the data file from disk, if they are not already
   * loaded
   *
   * @throws DataFileException
   *           If the file contents could not be loaded
   */
  private void loadContents() throws DataFileException {
    if (null == contents) {
      try {
        FileStore.loadFileContents(fileStore, this);

        if (!extractHeaderDate()) {
          throw new Exception("Could not extract file header date");
        }
      } catch (Exception e) {
        throw new DataFileException(databaseId,
          DataFileException.NO_LINE_NUMBER, "Error while loading file contents",
          e);
      }
    }
  }

  /**
   * Get a value from a field as a Double. If the extracted value equals the
   * {@code missingValue}, the method returns {@code null}.
   *
   * @param line
   *          The line
   * @param field
   *          The field index
   * @param missingValue
   *          The string indicating a missing value
   * @return The value
   * @throws DataFileException
   *           If the data cannot be extracted
   */
  public Double getDoubleValue(int line, int field, String missingValue)
    throws DataFileException {
    loadContents();
    String fieldValue = fileDefinition.extractFields(contents.get(line))
      .get(field);

    Double result = null;

    try {
      result = extractDoubleFieldValue(fieldValue, missingValue);
    } catch (ValueNotNumericException e) {
      throw new DataFileException(databaseId, line, e);
    }

    return result;
  }

  /**
   * Get a field value from a line. If the line does not have enough fields, or
   * the field is the defined missing value, returns {@code null}
   *
   * Any commas in a data value are assumed to be thousands separators and
   * removed from the string. QuinCe does not support commas as decimal points.
   *
   * @param line
   *          The line containing the value
   * @param field
   *          The field to retrieve
   * @param missingValue
   *          The defined missing value
   * @return The extracted value
   */
  public String getStringValue(String jobName, DataSet dataSet, int lineNumber,
    List<String> line, int field, String missingValue) {
    String result = null;

    if (field < line.size()) {
      result = line.get(field).trim().replaceAll(",", "");
      if (result.length() == 0 || result.equals(missingValue)
        || result.equalsIgnoreCase("NaN") || result.equalsIgnoreCase("NA")) {
        result = null;
      } else {

        // Strip leading zeros from integers - otherwise we get octal number
        // nonsense.
        if (!org.apache.commons.lang3.StringUtils.contains(result, '.')) {
          result = org.apache.commons.lang3.StringUtils.stripStart(result, "0");
        }

        if (!NumberUtils.isCreatable(result)) {
          dataSet.addProcessingMessage(jobName, this, lineNumber,
            "Invalid value '" + result + "'");
          result = null;
        }
      }
    }

    return result;
  }

  /**
   * Get the list of run type values with the specified value excluded. This
   * list will include all the run types from the stored file definition plus
   * any missing run types (except that specified as the exclusion).
   *
   * @param exclusion
   *          The value to exclude from the list
   * @return The list of run types without the excluded value
   */
  public List<String> getRunTypeValuesWithExclusion(String exclusion) {
    List<String> runTypeValues = fileDefinition.getRunTypeValues();
    for (RunTypeAssignment runTypeAssignment : missingRunTypes) {
      if (!runTypeAssignment.getRunName().equals(exclusion)) {
        runTypeValues.add(runTypeAssignment.getRunName());
      }
    }

    return runTypeValues;
  }

  /**
   * Get a line from the file as a list of field values
   *
   * @param line
   *          The line number
   * @return The line fields
   * @throws DataFileException
   */
  public List<String> getLine(int line) throws DataFileException {
    loadContents();
    return fileDefinition.extractFields(contents.get(line));
  }

  public Properties getProperties() {
    return properties;
  }

  public void setTimeOffset(int seconds) {
    properties.setProperty(TIME_OFFSET_PROP, String.valueOf(seconds));
  }

  public int getTimeOffset() {
    return Integer.parseInt(properties.getProperty(TIME_OFFSET_PROP));
  }

  public boolean hasTimeOffset() {
    return getTimeOffset() != 0;
  }

  private LocalDateTime applyTimeOffset(LocalDateTime rawTime) {
    return rawTime.plusSeconds(getTimeOffset());
  }

  @Override
  public String toString() {
    return filename;
  }
}
