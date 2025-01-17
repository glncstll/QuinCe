package uk.ac.exeter.QuinCe.utils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.math.NumberUtils;

/**
 * Miscellaneous string utilities.
 *
 * <p>
 * This class extends {@link org.apache.commons.lang3.StringUtils}, so methods
 * from that class can be called directly through this one.
 * </p>
 */
public final class StringUtils extends org.apache.commons.lang3.StringUtils {

  private static DecimalFormat threeDecimalPoints;

  static {
    threeDecimalPoints = new DecimalFormat();
    threeDecimalPoints.setMinimumFractionDigits(3);
    threeDecimalPoints.setMaximumFractionDigits(3);
    threeDecimalPoints.setGroupingUsed(false);
    threeDecimalPoints.setRoundingMode(RoundingMode.HALF_UP);
    threeDecimalPoints.setDecimalFormatSymbols(
      new DecimalFormatSymbols(new Locale("en", "US")));
  }

  /**
   * Private constructor to prevent instantiation
   */
  private StringUtils() {
    // Do nothing
  }

  /**
   * Converts a collection of values to a single string, with a specified
   * delimiter.
   *
   * <b>Note that this does not handle the case where the delimiter is found
   * within the values themselves.</b>
   *
   * @param collection
   *          The list to be converted
   * @param delimiter
   *          The delimiter to use
   * @return The converted list
   */
  public static String collectionToDelimited(Collection<?> collection,
    String delimiter) {

    String result = "";

    if (null != collection) {
      result = collection.stream().map(c -> c.toString())
        .collect(Collectors.joining(null == delimiter ? "" : delimiter));
    }

    return result;
  }

  public static String listToDelimited(List<String> list,
    TreeSet<Integer> entries, String delimiter) {

    List<String> selection = new ArrayList<String>();

    entries.forEach(e -> {
      selection.add(list.get(e));
    });

    return collectionToDelimited(selection, delimiter);
  }

  /**
   * Converts a String containing values separated a specified delimiter into a
   * list of String values.
   *
   * <p>
   * <strong>Limitations:</strong>
   * <ul>
   * <li>This does not handle escaped delimiters within the values
   * themselves.</li>
   * <li>Full stops/periods can be used as the delimeter, but other regex
   * special characters will not work.</li>
   * </ul>
   *
   * @param values
   *          The String to be converted
   * @param delimiter
   *          The delimiter
   * @return A list of String values
   * @see #checkDelimiter(String, String...)
   */
  public static List<String> delimitedToList(String values, String delimiter) {

    checkDelimiter(delimiter);

    List<String> result;

    if (null == values) {
      result = new ArrayList<String>(0);
    } else if (values.length() == 0) {
      result = new ArrayList<String>(0);
    } else {
      String regex = delimiter;
      if (delimiter.equals(".")) {
        regex = "\\.";
      }

      result = Arrays.asList(values.split(regex, 0));
    }

    return result;
  }

  /**
   * Convert a delimited list of integers into a list of integers
   *
   * @param values
   *          The list
   * @param delimiter
   *          The delimiter
   * @return The list as integers
   * @see #checkDelimiter(String, String...)
   */
  public static List<Integer> delimitedToIntegerList(String values,
    String delimiter) {

    checkDelimiter(delimiter, "-", ".");

    List<Integer> result;

    if (null == values || values.trim().length() == 0) {
      result = new ArrayList<Integer>(0);
    } else {
      String[] numberList = values.split(delimiter);
      result = new ArrayList<Integer>(numberList.length);

      for (String number : numberList) {
        result.add(Integer.parseInt(number));
      }
    }

    return result;
  }

  /**
   * Convert a delimited list of doubles (with {@code ;} separator) into a list
   * of doubles.
   *
   * @param values
   *          The delimited list.
   * @return The list of doubles.
   */
  public static List<Double> delimitedToDoubleList(String values) {
    return delimitedToDoubleList(values, ";");
  }

  /**
   * Convert a delimited list of double into a list of doubles
   *
   * @param values
   *          The list
   * @param delimiter
   *          The delimiter used in the input string.
   * @return The list as integers
   * @see #checkDelimiter(String, String...)
   */
  public static List<Double> delimitedToDoubleList(String values,
    String delimiter) {

    checkDelimiter(delimiter, "-", ".");

    List<Double> result;

    if (null == values || values.trim().length() == 0) {
      result = new ArrayList<Double>(0);
    } else {
      String[] numberList = values.split(delimiter);
      result = new ArrayList<Double>(numberList.length);

      for (String number : numberList) {
        result.add(Double.parseDouble(number));
      }
    }

    return result;
  }

  /**
   * Convert a comma-separated list of numbers to a list of longs
   *
   * @param values
   *          The numbers
   * @return The longs
   */
  public static List<Long> delimitedToLongList(String values) {
    // TODO This is the preferred way of doing this. Make the other methods do
    // the same.

    List<Long> result;

    if (null == values || values.trim().length() == 0) {
      result = new ArrayList<Long>(0);
    } else {
      String[] numberList = values.split(",");
      result = new ArrayList<Long>(numberList.length);

      for (String number : numberList) {
        result.add(Long.parseLong(number));
      }
    }

    return result;
  }

  /**
   * Convert a comma-separated list of numbers to a {@link Set} of longs. The
   * Set will be ordered.
   *
   * @param values
   *          The numbers.
   * @return The longs.
   */
  public static SortedSet<Long> delimitedToLongSet(String values) {
    TreeSet<Long> result = new TreeSet<Long>();

    if (null != values && values.trim().length() > 0) {
      String[] numberList = values.split(",");

      for (String number : numberList) {
        result.add(Long.parseLong(number));
      }
    }

    return result;
  }

  /**
   * Extract the stack trace from an Exception (or other Throwable) as a String.
   *
   * @param e
   *          The error
   * @return The stack trace
   */
  public static String stackTraceToString(Throwable e) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    e.printStackTrace(pw);
    return sw.toString();
  }

  /**
   * Trims all items in a list of strings. A string that starts with a single
   * backslash has that backslash removed.
   *
   * @param source
   *          The strings to be converted
   * @return The converted strings
   */
  public static List<String> trimList(List<String> source) {

    List<String> result = null;

    if (null != source) {
      result = source.stream().map(s -> {
        return trimString(s, "\\s");
      }).collect(Collectors.toList());
    }

    return result;
  }

  /**
   * Trims all items in a list of strings, and remotes any leading and/or
   * trailing double quotes.
   *
   * <ul>
   * <li>All whitespace and quotes are removed from the beginning and end of the
   * string.</li>
   * <li>If a single backslash remains on the front of the string, that is
   * removed also.</li>
   * <li>The process is repeated until the string is no longer modified.</li>
   * </ul>
   *
   * @param source
   *          The source list
   * @return The trimmed list
   */
  public static List<String> trimListAndQuotes(List<String> source) {

    List<String> result = null;

    if (null != source) {
      result = source.stream().map(s -> {
        return trimString(s, "\\s\"");
      }).collect(Collectors.toList());
    }

    return result;
  }

  private static String trimString(String string, String regexChars) {

    String trimmed = null;

    if (null != string) {
      trimmed = string
        .replaceAll("^[" + regexChars + "]*|[" + regexChars + "]*$", "");

      boolean done = false;
      while (!done) {
        if (trimmed.startsWith("\\\\")) {
          // If multiple \s, remove the first and stop
          trimmed = trimmed.substring(1);
          done = true;
        } else if (trimmed.startsWith("\\")) {
          // Trim off the single \ and trim the front again
          trimmed = trimmed.substring(1).replaceAll("^[" + regexChars + "]*",
            "");
        } else {
          done = true;
        }
      }
    }

    return trimmed;
  }

  /**
   * Convert a Properties object into a JSON string
   *
   * @param properties
   *          The properties
   * @return The JSON string
   */
  public static String getPropertiesAsJson(Properties properties) {

    StringBuilder result = new StringBuilder();
    if (null == properties) {
      result.append("null");
    } else {

      result.append('{');

      int propCount = 0;
      for (String prop : properties.stringPropertyNames()) {
        propCount++;
        result.append('"');
        result.append(prop);
        result.append("\":\"");
        result.append(properties.getProperty(prop));
        result.append('"');

        if (propCount < properties.size()) {
          result.append(',');
        }
      }

      result.append('}');
    }

    return result.toString();
  }

  /**
   * Create a {@link Properties} object from a string
   *
   * @param propsString
   *          The properties String
   * @return The Properties object
   * @throws IOException
   *           If the string cannot be parsed
   */
  public static Properties propertiesFromString(String propsString)
    throws IOException {
    Properties result = null;

    if (null != propsString && propsString.length() > 0) {
      StringReader reader = new StringReader(propsString);
      Properties props = new Properties();
      props.load(reader);
      return props;
    }

    return result;
  }

  /**
   * Make a valid CSV String from the given text.
   *
   * This always performs three steps:
   * <ul>
   * <li>Surround the value in quotes</li>
   * <li>Any " are replaced with "", per the CSV spec</li>
   * <li>Newlines are replaced with semi-colons</li>
   * </ul>
   *
   * While these are not strictly necessary for all values, they are appropriate
   * for this application and the target audiences of exported CSV files.
   *
   * @param text
   *          The value
   * @return The CSV value
   */
  public static String makeCsvString(String text) {
    StringBuilder csv = new StringBuilder();

    if (null == text) {
      text = "";
    }

    csv.append('"');
    csv.append(text.trim().replace("\"", "\"\"").replaceAll("[\\r\\n]+", ";"));
    csv.append('"');

    return csv.toString();
  }

  /**
   * Generate a Double value from a String, handling thousands separators
   *
   * @param value
   *          The string value
   * @return The double value
   */
  public static Double doubleFromString(String value) {
    Double result = Double.NaN;
    if (null != value && value.trim().length() > 0) {
      result = Double.parseDouble(value.replaceAll(",", "").trim());
    }

    return result;
  }

  public static String formatNumber(String value) {
    String result = value;

    if (NumberUtils.isCreatable(value)) {
      result = threeDecimalPoints.format(new BigDecimal(value));
    }

    return result;
  }

  public static String formatNumber(Double value) {
    String result = null;

    if (null != value) {
      result = threeDecimalPoints.format(value);
    }

    return result;
  }

  public static String tabToSpace(String in) {

    String result = null;

    if (null != in) {
      return in.replaceAll("\t", " ");
    }

    return result;
  }

  /**
   * Check whether or not a specified delimiter is valid.
   *
   * <p>
   * The following delimiters are invalid:
   * </p>
   * <ul>
   * <li>{@code null}</li>
   * <li>Double or single quotes</li>
   * <li>The digits 0-9</li>
   * <li>Any strings specified in {@code invalidDelimiters}</li>
   * </ul>
   *
   * <p>
   * The method throws an {@link IllegalArgumentException} if the delimiter is
   * invalid.
   * </p>
   *
   * @param delimiter
   *          The delimiter to test.
   * @param invalidDelimiters
   *          The additional invalid delimiters
   */
  private static void checkDelimiter(String delimiter,
    String... invalidDelimiters) {

    if (null == delimiter) {
      throw new IllegalArgumentException("null delimiter is invalid");
    }

    if (delimiter.length() != 1) {
      throw new IllegalArgumentException("Delimiter must be one character");
    }

    if (Pattern.matches("[\"'0-9a-zA-Z]", delimiter)) {
      throw new IllegalArgumentException(
        "Invalid delimiter '" + delimiter + "'");
    }

    for (int i = 0; i < invalidDelimiters.length; i++) {
      if (delimiter.equals(invalidDelimiters[i])) {
        throw new IllegalArgumentException(
          "Invalid delimiter '" + delimiter + "'");
      }
    }
  }

  /**
   * Sort a list of {@link String}s by length.
   *
   * <p>
   * Nulls are considered to be shorter than zero-length strings. The ordering
   * of strings of the same length in the sorted list is not defined.
   * </p>
   *
   * @param list
   *          The list to be sorted.
   * @param descending
   *          Indicates whether the list entries should be sorted by descending
   *          length.
   *
   * @see DescendingLengthComparator
   * @see AscendingLengthComparator
   */
  public static void sortByLength(List<String> list, boolean descending) {

    Comparator<String> comparator = descending
      ? new DescendingLengthComparator()
      : new AscendingLengthComparator();

    Collections.sort(list, comparator);
  }

  /**
   * Format a {@link String} so it can be parsed by Javascript in a literal
   * string argument.
   *
   * <p>
   * Replaces {@code '} with {@code \'}.
   * </p>
   *
   * @param string
   *          The String to be converted.
   * @return The converted String.
   */
  public static String javascriptString(String string) {
    return string.replaceAll("'", Matcher.quoteReplacement("\\'"));
  }

  public static String replaceNewlines(String str) {
    return null == str ? null : str.replaceAll("\\r?\\n", ";");
  }

  public static void removeBlankTailLines(List<String> list) {
    boolean blankLine = true;
    while (blankLine) {
      String lastLine = list.get(list.size() - 1);
      if (lastLine.trim().length() == 0) {
        list.remove(list.size() - 1);
      } else {
        blankLine = false;
      }
    }
  }
}
