package uk.ac.exeter.QuinCe.data.Dataset.QC;

/**
 * Represents a Flag placed on a data record.
 * <p>
 * Flags are based on the WOCE flags, with values for Good, Questionable and
 * Bad. All records exported from a system should ultimately have one of these
 * three flags assigned to it. However, during processing a number of other flag
 * values can be useful:
 * </p>
 * <ul>
 * <li><b>NO QC:</b> No QC has been performed.</li>
 * <li><b>Flushing:</b> The instrument is in Flushing mode, so values should be
 * ignored.</li>
 * <li><b>Assumed Good:</b> Automatic processing has flagged the record as good,
 * and there is no indication that this should be changed.</li>
 * <li><b>Needed:</b> A flag must be assigned manually by a human.</li>
 * <li><b>Lookup:</b> The QC for this value is inherited from another value. The
 * value ID(s) are stored in the comment.</li>
 * </ul>
 *
 * @author Steve Jones
 */
public class Flag implements Comparable<Flag> {

  /**
   * Value indicating that no QC has been performed
   */
  public static final int VALUE_NO_QC = 0;

  /**
   * The WOCE value for a good flag
   */
  public static final int VALUE_GOOD = 2;

  /**
   * The text value for a good flag
   */
  protected static final String TEXT_GOOD = "Good";

  /**
   * The WOCE value for a good flag
   */
  public static final int VALUE_ASSUMED_GOOD = -2;

  /**
   * The text indicating that no QC has been performed
   */
  public static final String TEXT_NO_QC = "No QC";

  /**
   * The text value for a good flag
   */
  protected static final String TEXT_ASSUMED_GOOD = "Assumed Good";

  /**
   * The WOCE value for a questionable flag
   */
  public static final int VALUE_QUESTIONABLE = 3;

  /**
   * The text value for a questionable flag
   */
  protected static final String TEXT_QUESTIONABLE = "Questionable";

  /**
   * The WOCE value for a bad flag
   */
  public static final int VALUE_BAD = 4;

  /**
   * The text value for a bad flag
   */
  protected static final String TEXT_BAD = "Bad";

  /**
   * The special value for a needed flag
   */
  public static final int VALUE_NEEDED = -10;

  /**
   * The text value for a needed flag
   */
  protected static final String TEXT_NEEDED = "Needed";

  /**
   * The special flag for a sensor value taken during the instrument's flushing
   * period
   */
  public static final int VALUE_FLUSHING = -100;

  /**
   * The text value for a flushing period flag
   */
  protected static final String TEXT_FLUSHING = "In flushing time";

  /**
   * Flag indicating that the Auto QC flag must be used and cannot be
   * overridden.
   */
  public static final int VALUE_LOOKUP = -200;

  /**
   * Text value for the Auto QC flag.
   */
  protected static final String TEXT_LOOKUP = "Lookup";

  /**
   * An instance of a No QC flag
   */
  public static final Flag NO_QC = makeNoQCFlag();

  /**
   * An instance of a Good flag
   */
  public static final Flag GOOD = makeGoodFlag();

  /**
   * An instance of an Assumed Good flag
   */
  public static final Flag ASSUMED_GOOD = makeAssumedGoodFlag();

  /**
   * An instance of a Questionable flag
   */
  public static final Flag QUESTIONABLE = makeQuestionableFlag();

  /**
   * An instance of a Bad flag
   */
  public static final Flag BAD = makeBadFlag();

  /**
   * An instance of a Needed flag
   */
  public static final Flag NEEDED = makeNeededFlag();

  /**
   * An instance of a Flushing flag
   */
  public static final Flag FLUSHING = makeFlushingFlag();

  /**
   * An instance of an Auto QC flag
   */
  public static final Flag LOOKUP = makeLookupQCFlag();

  /**
   * The WOCE value for this flag
   */
  protected int flagValue;

  /**
   * Creates a Flag instance with the specified value
   *
   * @param flagValue
   *          The flag's WOCE value
   * @throws InvalidFlagException
   *           If the flag value is invalid
   */
  public Flag(int flagValue) throws InvalidFlagException {
    if (!isValidFlagValue(flagValue)) {
      throw new InvalidFlagException(flagValue);
    }

    this.flagValue = flagValue;
  }

  public Flag(char flagLetter) throws InvalidFlagException {
    switch (Character.toUpperCase(flagLetter)) {
    case 'G':
    case '2': {
      this.flagValue = VALUE_GOOD;
      break;
    }
    case 'A': {
      this.flagValue = VALUE_ASSUMED_GOOD;
      break;
    }
    case 'Q':
    case '3': {
      this.flagValue = VALUE_QUESTIONABLE;
      break;
    }
    case 'B':
    case '4': {
      this.flagValue = VALUE_BAD;
      break;
    }
    case 'N': {
      this.flagValue = VALUE_NEEDED;
      break;
    }
    case 'F': {
      this.flagValue = VALUE_FLUSHING;
      break;
    }
    case 'X': {
      this.flagValue = VALUE_NO_QC;
      break;
    }
    case 'L': {
      this.flagValue = VALUE_LOOKUP;
      break;
    }
    default: {
      throw new InvalidFlagException(flagLetter);
    }
    }

  }

  /**
   * Create a flag based on an existing flag. Used by internal classes only
   *
   * @param sourceFlag
   *          The source flag
   */
  protected Flag(Flag sourceFlag) {
    this.flagValue = sourceFlag.flagValue;
  }

  /**
   * Returns the flag's numeric value
   *
   * @return The flag's numeric value
   */
  public int getFlagValue() {
    return flagValue;
  }

  /**
   * Converts the flag's numeric value into a String value
   */
  @Override
  public String toString() {
    String result;

    switch (flagValue) {
    case VALUE_NO_QC: {
      result = TEXT_NO_QC;
      break;
    }
    case VALUE_GOOD: {
      result = TEXT_GOOD;
      break;
    }
    case VALUE_ASSUMED_GOOD: {
      result = TEXT_ASSUMED_GOOD;
      break;
    }
    case VALUE_QUESTIONABLE: {
      result = TEXT_QUESTIONABLE;
      break;
    }
    case VALUE_BAD: {
      result = TEXT_BAD;
      break;
    }
    case VALUE_NEEDED: {
      result = TEXT_NEEDED;
      break;
    }
    case VALUE_FLUSHING: {
      result = TEXT_FLUSHING;
      break;
    }
    case VALUE_LOOKUP: {
      result = TEXT_LOOKUP;
      break;
    }
    default: {
      // This should never happen!
      result = "***INVALID FLAG VALUE***";
    }
    }

    return result;
  }

  /**
   * Checks to ensure that a flag value is valid. If the value is valid, the
   * method does nothing. If it is not valid, an exception is thrown.
   *
   * @param value
   *          The flag value
   * @return {@code true} if the flag value is valid; {@code false} if it is not
   */
  public static boolean isValidFlagValue(int value) {
    return (value == VALUE_NO_QC || value == VALUE_GOOD
      || value == VALUE_ASSUMED_GOOD || value == VALUE_QUESTIONABLE
      || value == VALUE_BAD || value == VALUE_NEEDED || value == VALUE_FLUSHING
      || value == VALUE_LOOKUP);
  }

  /**
   * Create an instance of a Good flag
   *
   * @return A Good flag
   */
  private static Flag makeNoQCFlag() {
    Flag flag = null;
    try {
      flag = new Flag(VALUE_NO_QC);
    } catch (InvalidFlagException e) {
      // This won't be thrown; do nothing
    }

    return flag;
  }

  /**
   * Create an instance of a Good flag
   *
   * @return A Good flag
   */
  private static Flag makeGoodFlag() {
    Flag flag = null;
    try {
      flag = new Flag(VALUE_GOOD);
    } catch (InvalidFlagException e) {
      // This won't be thrown; do nothing
    }

    return flag;
  }

  /**
   * Create an instance of a Good flag
   *
   * @return A Good flag
   */
  private static Flag makeAssumedGoodFlag() {
    Flag flag = null;
    try {
      flag = new Flag(VALUE_ASSUMED_GOOD);
    } catch (InvalidFlagException e) {
      // This won't be thrown; do nothing
    }

    return flag;
  }

  /**
   * Create an instance of a Questionable flag
   *
   * @return A Questionable flag
   */
  private static Flag makeQuestionableFlag() {
    Flag flag = null;
    try {
      flag = new Flag(VALUE_QUESTIONABLE);
    } catch (InvalidFlagException e) {
      // This won't be thrown; do nothing
    }

    return flag;
  }

  /**
   * Create an instance of a Bad flag
   *
   * @return A Bad flag
   */
  private static Flag makeBadFlag() {
    Flag flag = null;
    try {
      flag = new Flag(VALUE_BAD);
    } catch (InvalidFlagException e) {
      // This won't be thrown; do nothing
    }

    return flag;
  }

  /**
   * Create an instance of a Not Set flag
   *
   * @return A Not Set flag
   */
  private static Flag makeNeededFlag() {
    Flag flag = null;
    try {
      flag = new Flag(VALUE_NEEDED);
    } catch (InvalidFlagException e) {
      // This won't be thrown; do nothing
    }

    return flag;
  }

  /**
   * Create an instance of a Flushing flag
   *
   * @return A Flushing flag
   */
  private static Flag makeFlushingFlag() {
    Flag flag = null;
    try {
      flag = new Flag(VALUE_FLUSHING);
    } catch (InvalidFlagException e) {
      // This won't be thrown; do nothing
    }

    return flag;
  }

  /**
   * Create an instance of an Auto QC flag
   *
   * @return An Auto QC flag
   */
  private static Flag makeLookupQCFlag() {
    Flag flag = null;
    try {
      flag = new Flag(VALUE_LOOKUP);
    } catch (InvalidFlagException e) {
      // This won't be thrown; do nothing
    }

    return flag;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + flagValue;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof Flag))
      return false;
    Flag other = (Flag) obj;
    if (flagValue != other.flagValue)
      return false;
    return true;
  }

  @Override
  public int compareTo(Flag flag) {
    return this.flagValue - flag.flagValue;
  }

  /**
   * Determines whether or not this flag is more significant than the specified
   * flag. The order of significance for flags is (lowest significance first):
   * Not Set, Good, Questionable, Bad, Needed
   *
   * @param flag
   *          The flag to be compared
   * @return {@code true} if this flag is more significant than the supplied
   *         flag; {@code false} if it is not.
   */
  public boolean moreSignificantThan(Flag flag) {
    boolean result = false;

    // FLUSHING > NEEDED > everything else
    if (null == flag) {
      result = true;
    } else if (flag.equals(Flag.LOOKUP)) {
      result = false;
    } else if (this.equals(Flag.LOOKUP)) {
      result = true;
    } else if (flag.equals(Flag.FLUSHING)) {
      result = false;
    } else if (this.equals(Flag.FLUSHING)) {
      result = true;
    } else if (flag.equals(Flag.NEEDED)) {
      result = false;
    } else if (this.equals(Flag.NEEDED)) {
      result = true;
    } else {
      result = (compareTo(flag) > 0);
    }

    return result;
  }

  /**
   * Determines whether or not this flag is less significant than the specified
   * flag.
   *
   * @param flag
   *          The flag to be compared.
   * @return {@code true} if this flag is less significant than the supplied
   *         flag; {@code false} if it is not.
   */
  public boolean lessSignificantThan(Flag flag) {
    return !moreSignificantThan(flag) && !equalSignificance(flag);
  }

  /**
   * Determines whether or not this flag represents a Good value. Both Good and
   * Assumed Good flags pass the check.
   *
   * @return {@code true} if this flag is Good; {@code false} if it is not.
   */
  public boolean isGood() {
    return Math.abs(flagValue) == VALUE_GOOD;
  }

  /**
   * Return the WOCE value for a flag.
   *
   * @return The WOCE value for the flag
   * @see #getWoceValue(int)
   */
  public int getWoceValue() {
    return getWoceValue(flagValue);
  }

  /**
   * Return the WOCE value for a given flag value
   * <ul>
   * <li>Good and Assumed Good will return 2</li>
   * <li>Questionable will return 3</li>
   * <li>Bad and Fatal will return 4</li>
   * <li>All other flag types will return -1, because there is no corresponding
   * WOCE value.</li>
   * </ul>
   *
   * @param flagValue
   *          The numeric flag value
   * @return The WOCE value for the flag
   */
  public static int getWoceValue(int flagValue) {
    int result;

    switch (flagValue) {
    case VALUE_GOOD:
    case VALUE_ASSUMED_GOOD: {
      result = 2;
      break;
    }
    case VALUE_QUESTIONABLE: {
      result = 3;
      break;
    }
    case VALUE_BAD: {
      result = 4;
      break;
    }
    default: {
      result = -1;
    }
    }

    return result;
  }

  /**
   * Check whether the supplied flag is of equal significance to this flag.
   * <p>
   * Uses the output of {@link #getWoceValue()} for comparison. A {@code false}
   * result does not indicate which flag is more significant; use
   * {@link #moreSignificantThan(Flag)}.
   * </p>
   *
   * @param otherFlag
   *          The flag to be compared.
   * @return {@code true} if the supplied flag is of equal significance to this
   *         flag; {@code false} otherwise.
   */
  public boolean equalSignificance(Flag otherFlag) {
    return otherFlag.getWoceValue() == getWoceValue();
  }

  /**
   * Get the fully simplified version of the Flag.
   *
   * <p>
   * Returns the basic {@link Flag} object on which a subclassed {@link Flag} is
   * based.
   * </p>
   *
   * @return The raw Flag object.
   */
  public Flag getSimpleFlag() {
    return this;
  }

  /**
   * Get the flag with the highest significance from a the supplied {@link Flag}
   * objects.
   *
   * @param flags
   *          The flags to check.
   * @return The most significant flag.
   */
  public static Flag getWorstFlag(Flag... flags) {
    Flag result = Flag.GOOD;

    for (Flag flag : flags) {
      if (flag.moreSignificantThan(result)) {
        result = flag;
      }
    }

    return result;
  }
}
