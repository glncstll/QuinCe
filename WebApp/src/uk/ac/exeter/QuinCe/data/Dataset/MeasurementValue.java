package uk.ac.exeter.QuinCe.data.Dataset;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.StringUtils;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageTableValue;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Holds the calculated value of a given {@link SensorType} for a measurement.
 * <p>
 * The calculated value is derived from one or more {@link SensorValue}s,
 * depending on the configuration of the instrument, the relative time of the
 * measurement and available {@link SensorValue}s, and whether
 * {@link Flag#BAD}/{@link Flag#QUESTIONABLE} values are being ignored.
 * </p>
 * <p>
 * This class implements the most common calculation of a measurement value.
 * Some {@link SensorType}s require more complex calculations (e.g. xCO₂ with
 * standards, which also requires xH₂O). These can be implemented by overriding
 * classes.
 * </p>
 *
 * @author Steve Jones
 */
public class MeasurementValue implements PlotPageTableValue {

  /**
   * The {@link SensorType} that this measurement value is for.
   */
  private final SensorType sensorType;

  /**
   * The IDs of the {@link SensorValue}s used to calculate the value.
   * <p>
   * Note that the sensor values may not all belong to the specified
   * {@link #sensorType}: some {@link SensorType}s require calculation based on
   * other sensors (e.g. CO2 requires xH2O).
   * </p>
   */
  private List<Long> sensorValueIds;

  /**
   * The IDs of {@link SensorValue}s used to support the value calculation.
   * <p>
   * This can be used for values that aren't directly used in the calculation,
   * such as those used in calibrations.
   * </p>
   */
  private List<Long> supportingSensorValueIds;

  /**
   * The number of calculations used to build this value.
   * <p>
   * This is useful for combining multiple values, where a weighted mean is
   * often required.
   * </p>
   */
  private int memberCount = 0;

  /**
   * The calculated value for the sensor type.
   */
  private Double calculatedValue = Double.NaN;

  /**
   * The value type
   */
  private char type = NAN_TYPE;

  /**
   * The QC flag for this value, derived from the contributing
   * {@link SensorValues}.
   */
  private Flag flag = Flag.ASSUMED_GOOD;

  /**
   * The QC message for this value, derived from the contributing
   * {@link SensorValues}.
   */
  private HashSet<String> qcMessage;

  /**
   * Miscellaneous properties
   */
  private Properties properties;

  /**
   * Creates a stub value with no assigned {@link SensorValue}s.
   *
   * @param sensorType
   *          The sensor type that the value is calculated for.
   */
  public MeasurementValue(SensorType sensorType) {
    this.sensorType = sensorType;
    this.sensorValueIds = new ArrayList<Long>();
    this.supportingSensorValueIds = new ArrayList<Long>();
    this.qcMessage = new HashSet<String>();
    this.properties = new Properties();
  }

  /**
   * Construct a MeasurementValue using a full set of fields.
   *
   * @param sensorTypeId
   * @param sensorValueIds
   * @param supportingSensorValueIds
   * @param calculatedValue
   * @param flag
   * @param qcComments
   * @param properties
   * @throws SensorTypeNotFoundException
   */
  public MeasurementValue(long sensorTypeId, List<Long> sensorValueIds,
    List<Long> supportingSensorValueIds, int memberCount,
    Double calculatedValue, Flag flag, HashSet<String> qcComments, char type,
    Properties properties) throws SensorTypeNotFoundException {

    this.sensorType = ResourceManager.getInstance().getSensorsConfiguration()
      .getSensorType(sensorTypeId);
    this.sensorValueIds = sensorValueIds;
    this.supportingSensorValueIds = supportingSensorValueIds;
    this.memberCount = memberCount;
    this.calculatedValue = calculatedValue;
    this.flag = flag;
    this.qcMessage = qcComments;
    this.type = type;
    this.properties = properties;
  }

  /**
   * Construct a MeasurementValue, calculating the QC details from the supplied
   * {@link SensorValue}s.
   *
   * @param sensorType
   * @param sensorValues
   * @param calculatedValue
   * @param memberCount
   * @throws RoutineException
   */
  public MeasurementValue(SensorType sensorType, List<SensorValue> sensorValues,
    List<SensorValue> supportingSensorValues,
    DatasetSensorValues allSensorValues, Double calculatedValue,
    int memberCount, char type) throws RoutineException {

    this.sensorType = sensorType;
    this.sensorValueIds = new ArrayList<Long>();
    this.memberCount = memberCount;
    this.supportingSensorValueIds = new ArrayList<Long>();
    this.supportingSensorValueIds = new ArrayList<Long>();
    this.calculatedValue = calculatedValue;
    this.memberCount = memberCount;
    this.qcMessage = new HashSet<String>();
    this.properties = new Properties();
    addSensorValues(sensorValues, allSensorValues, false);
    addSupportingSensorValues(supportingSensorValues);
    this.type = type;
  }

  /**
   * Adds multiple {@link SensorValue}s to this {@code MeasurementValue}.
   * <p>
   * If any supplied values are {@code null}, they are not added.
   * </p>
   * <p>
   * Setting {@code incrMemberCount = true} always causes the {@code #type} to
   * be set to {@code PlotPageTableValue#INTERPOLATED_TYPE}, even if only one
   * non-{@code null} value is passed. There is an implicit assumption that
   * calling this method was the result of an attempted interpolation; if a
   * requested timestamp is outside the bounds of available
   * {@link SensorValue}s, the first or last value will have been used, but it
   * should still count as an interpolation since it has been extrapolated.
   * </p>
   *
   * @param values
   *          The values to be added.
   * @param incrMemberCount
   *          Indicates whether or not the member count should be incremented.
   * @throws RoutineException
   */
  private void addSensorValues(Collection<SensorValue> values,
    DatasetSensorValues allSensorValues, boolean incrMemberCount)
    throws RoutineException {

    for (SensorValue value : values) {
      addSensorValue(value, allSensorValues, incrMemberCount);
    }

    if (incrMemberCount) {
      type = INTERPOLATED_TYPE;
    }
  }

  /**
   * Add the sensor values used in the specified {@code MeasurementValue}s.
   *
   * @param sourceValues
   *          The source {@code MeasurementValue}s.
   * @throws RoutineException
   */
  public void addSensorValuesFromMeasurementValue(
    Collection<MeasurementValue> sourceValues,
    DatasetSensorValues allSensorValues) throws RoutineException {

    for (MeasurementValue measurementValue : sourceValues) {
      addSensorValues(measurementValue, allSensorValues);
    }
  }

  /**
   * Add the sensor values used in the specified {@code MeasurementValue}.
   *
   * @param sourceValues
   *          The source {@code MeasurementValue}.
   * @throws RoutineException
   */
  public void addSensorValues(MeasurementValue sourceValue,
    DatasetSensorValues allSensorValues) throws RoutineException {
    for (Long sensorValueId : sourceValue.getSensorValueIds()) {
      addSensorValue(allSensorValues.getById(sensorValueId), allSensorValues);
    }
  }

  /**
   * Add the sensor values used in the specified {@code MeasurementValue} as
   * supporting sensor values.
   *
   * @param sourceValues
   *          The source {@code MeasurementValue}.
   */
  public void addSupportingSensorValues(MeasurementValue sourceValue,
    DatasetSensorValues allSensorValues) {
    for (Long sensorValueId : sourceValue.getSensorValueIds()) {
      addSupportingSensorValue(allSensorValues.getById(sensorValueId));
    }
  }

  /**
   * Add a {@link SensorValue} to the value.
   * <p>
   * Adds the {@link SensorValue}'s ID and updates the QC information.
   * </p>
   *
   * @param value
   *          The value to add.
   * @param incrMemberCount
   *          Indicates whether or not this value should contribute to the
   *          member count.
   * @throws RoutineException
   */
  private void addSensorValue(SensorValue value,
    DatasetSensorValues allSensorValues, boolean incrMemberCount)
    throws RoutineException {
    if (null != value) {
      if (!sensorValueIds.contains(value.getId())) {
        sensorValueIds.add(value.getId());

        Flag valueFlag = value.getDisplayFlag().getSimpleFlag();

        if (valueFlag.equals(flag)) {
          if (value.getUserQCMessage().trim().length() > 0) {
            qcMessage.add(value.getDisplayQCMessage(allSensorValues));
          }
        } else if (valueFlag.moreSignificantThan(flag)) {
          flag = valueFlag;
          qcMessage.clear();

          if (value.getUserQCMessage().trim().length() > 0) {
            qcMessage.add(value.getDisplayQCMessage(allSensorValues));
          }
        }

        if (incrMemberCount) {
          memberCount++;
          type = memberCount == 1 ? MEASURED_TYPE : INTERPOLATED_TYPE;
        }
      }
    }
  }

  public void addSensorValue(SensorValue sensorValue,
    DatasetSensorValues allSensorValues) throws RoutineException {
    addSensorValue(sensorValue, allSensorValues, true);
  }

  public void addSensorValues(Collection<SensorValue> sensorValues,
    DatasetSensorValues allSensorValues) throws RoutineException {
    addSensorValues(sensorValues, allSensorValues, true);
  }

  /**
   * Add a {@link SensorValue} to the value and force the {@link #type} to be
   * {@link PlotPageTableValue#INTERPOLATED_TYPE} regardless of the resulting
   * {@link #memberCount}.
   *
   * @param value
   *          The value to add.
   * @param incrMemberCount
   *          Indicates whether or not this value should contribute to the
   *          member count.
   * @throws RoutineException
   */
  public void addInterpolatedSensorValue(SensorValue value,
    DatasetSensorValues allSensorValues, boolean incrMemberCount)
    throws RoutineException {
    addSensorValue(value, allSensorValues, incrMemberCount);
    type = INTERPOLATED_TYPE;
  }

  public void addSupportingSensorValue(SensorValue value) {
    supportingSensorValueIds.add(value.getId());
  }

  public void addSupportingSensorValues(Collection<SensorValue> values) {
    if (null != values) {
      values.forEach(this::addSupportingSensorValue);
    }
  }

  public List<Long> getSupportingSensorValueIds() {
    return supportingSensorValueIds;
  }

  public void setCalculatedValue(Double calculatedValue) {
    this.calculatedValue = null == calculatedValue ? Double.NaN
      : calculatedValue;
  }

  public Double getCalculatedValue() {
    return calculatedValue;
  }

  /**
   * Get the QC flag for this value. If the {@link #calculatedValue} is
   * {@link Double#NaN}, the flag is always {@link Flag#BAD}.
   *
   * @return The QC flag.
   */
  public Flag getQcFlag() {
    return calculatedValue.isNaN() ? Flag.BAD : flag;
  }

  /**
   * Get the QC messages for this value as a {@link Set}. If the
   * {@link #calculatedValue} is {@link Double#NaN}, the list is a single value
   * of {@code "NaN"}.
   *
   * @return The QC messages.
   */
  public Set<String> getQcMessages() {
    Set<String> result = qcMessage;

    if (calculatedValue.isNaN()) {
      result = new HashSet<String>();
      result.add("NaN");
    }

    return result;
  }

  public SensorType getSensorType() {
    return sensorType;
  }

  public boolean hasValue() {
    return sensorValueIds.size() > 0;
  }

  public int getMemberCount() {
    return memberCount;
  }

  public List<Long> getSensorValueIds() {
    return sensorValueIds;
  }

  protected Properties getProperties() {
    return properties;
  }

  public void setProperty(String key, String value) {
    properties.setProperty(key, value);
  }

  public String getProperty(String key) {
    return properties.getProperty(key);
  }

  /**
   * Replace the existing QC information with the supplied flag and QC message.
   *
   * @param flag
   *          The new QC flag.
   * @param message
   *          The new QC message.
   */
  public void overrideQC(Flag flag, String message) {
    this.flag = flag;
    this.qcMessage = new HashSet<String>();
    this.qcMessage.add(message);
  }

  /**
   * Replace the existing QC information with the supplied flag and QC message.
   *
   * @param flag
   *          The new QC flag.
   * @param message
   *          The new QC messages.
   */
  public void overrideQC(Flag flag, Collection<String> messages) {
    this.flag = flag;
    this.qcMessage = new HashSet<String>(messages);
  }

  /**
   * Add a message to the override QC messages.
   *
   * @param message
   *          The message.
   */
  public void addQcMessage(String message) {
    if (null == qcMessage) {
      qcMessage = new HashSet<String>();
    }
    qcMessage.add(message);
  }

  /**
   * Manually set the type of this {@link MeasurementValue}.
   *
   * @param type
   *          The new type
   * @throws MeasurementValueException
   *           If the type is invalid
   */
  public void setType(char type) throws MeasurementValueException {

    boolean ok = true;
    String errorMessage = null;

    switch (type) {
    case PlotPageTableValue.MEASURED_TYPE: {
      if (memberCount > 0 || sensorValueIds.size() > 0) {
        ok = false;
        errorMessage = "Cannot set Measured type for multiple members";
      }
      break;
    }
    case PlotPageTableValue.INTERPOLATED_TYPE: {
      // This is always allowed
      break;
    }
    case PlotPageTableValue.DATA_REDUCTION_TYPE:
    case PlotPageTableValue.NAN_TYPE:
    case PlotPageTableValue.NOMINAL_TYPE: {
      ok = false;
      errorMessage = "MeasurementValue type '" + type
        + "' cannot be set manually";
    }
    default: {
      ok = false;
      errorMessage = "Unrecognised MeasurementValue type '" + type + "'";

    }
    }

    if (!ok) {
      throw new MeasurementValueException(errorMessage);
    } else {
      this.type = type;
    }
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result
      + ((calculatedValue == null) ? 0 : calculatedValue.hashCode());
    result = prime * result
      + ((sensorType == null) ? 0 : sensorType.hashCode());
    result = prime * result
      + ((sensorValueIds == null) ? 0 : sensorValueIds.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    MeasurementValue other = (MeasurementValue) obj;
    if (calculatedValue == null) {
      if (other.calculatedValue != null)
        return false;
    } else if (!calculatedValue.equals(other.calculatedValue))
      return false;
    if (sensorType == null) {
      if (other.sensorType != null)
        return false;
    } else if (!sensorType.equals(other.sensorType))
      return false;
    if (sensorValueIds == null) {
      if (other.sensorValueIds != null)
        return false;
    } else if (!sensorValueIds.equals(other.sensorValueIds))
      return false;
    return true;
  }

  @Override
  public long getId() {
    return DatabaseUtils.NO_DATABASE_RECORD;
  }

  @Override
  public String getValue() {
    return calculatedValue.isNaN() ? null
      : StringUtils.formatNumber(calculatedValue);
  }

  @Override
  public Object getRawValue() {
    return calculatedValue;
  }

  @Override
  public String getQcMessage(DatasetSensorValues allSensorValues,
    boolean replaceNewlines) {
    String result = StringUtils.collectionToDelimited(getQcMessages(), ";");
    if (replaceNewlines) {
      result = StringUtils.replaceNewlines(result);
    }

    return result;
  }

  @Override
  public boolean getFlagNeeded() {
    return false;
  }

  @Override
  public boolean isNull() {
    return calculatedValue.isNaN();
  }

  @Override
  public char getType() {
    return calculatedValue.isNaN() ? NAN_TYPE : type;
  }

  @Override
  public String toString() {
    return sensorType.getShortName() + " = " + calculatedValue;
  }

  @Override
  public Collection<Long> getSources() {
    return sensorValueIds;
  }
}
