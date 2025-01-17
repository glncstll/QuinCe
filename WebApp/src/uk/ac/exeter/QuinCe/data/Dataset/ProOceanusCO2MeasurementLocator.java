package uk.ac.exeter.QuinCe.data.Dataset;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Measurement locator for Pro Oceanus CO2 sensors. Handles both marine and
 * atmospheric variables (use a dummy locator for the actual Atmospheric
 * variable).
 *
 * @author stevej
 *
 */
public class ProOceanusCO2MeasurementLocator extends MeasurementLocator {

  // Run types are converted to lower case

  private static final String WATER_MODE = "w m";

  private static final String ATM_MODE = "a m";

  @Override
  public List<Measurement> locateMeasurements(Connection conn,
    Instrument instrument, DataSet dataset) throws MeasurementLocatorException {

    try {
      SensorsConfiguration sensorConfig = ResourceManager.getInstance()
        .getSensorsConfiguration();

      Variable waterVar = sensorConfig
        .getInstrumentVariable("Pro Oceanus CO₂ Water");
      Variable atmVar = sensorConfig
        .getInstrumentVariable("Pro Oceanus CO₂ Atmosphere");

      HashMap<Long, String> waterRunTypes = new HashMap<Long, String>();
      waterRunTypes.put(waterVar.getId(), Measurement.MEASUREMENT_RUN_TYPE);

      HashMap<Long, String> atmRunTypes = new HashMap<Long, String>();
      atmRunTypes.put(atmVar.getId(), Measurement.MEASUREMENT_RUN_TYPE);

      SensorType zeroCountType = sensorConfig
        .getSensorType("ProOceanus Zero Count");
      SensorType co2Type = sensorConfig
        .getSensorType("xCO₂ (wet, no standards)");

      // Assume one column of each type
      long zeroCountColumn = instrument.getSensorAssignments()
        .getColumnIds(zeroCountType).get(0);
      long co2Column = instrument.getSensorAssignments().getColumnIds(co2Type)
        .get(0);

      TreeMap<LocalDateTime, String> runTypes = getRunTypes(conn, dataset,
        instrument);

      DatasetSensorValues sensorValues = DataSetDataDB.getSensorValues(conn,
        instrument, dataset.getId(), false, true);

      // Loop through all the rows, examining the zero/flush columns to decide
      // what to do
      List<SensorValue> flaggedSensorValues = new ArrayList<SensorValue>();
      List<Measurement> measurements = new ArrayList<Measurement>(
        sensorValues.getTimes().size());

      String lastZero = null;
      LocalDateTime flushingEndTime = LocalDateTime.MIN;

      for (LocalDateTime recordTime : sensorValues.getTimes()) {
        Map<Long, SensorValue> recordValues = sensorValues.get(recordTime);

        String runType = runTypes.get(recordTime);
        // Null run types can happen if data is coming from multiple files (eg
        // TSG data)
        // The TSG will have timestamps that don't match up to the Pro Oceanus
        // data,
        // so they won't have a run type.
        if (null != runType && !runType.equals("")
          && !runType.equalsIgnoreCase("NaN")) {

          if (!runType.equals(WATER_MODE) && !runType.equals(ATM_MODE)) {
            throw new MeasurementLocatorException(
              "Unrecognised ProOceanus mode '" + runType + "'");
          }

          String zero = recordValues.get(zeroCountColumn).getValue();
          if (!zero.equals(lastZero)) {
            flushingEndTime = recordTime.plusSeconds(
              instrument.getIntProperty(Instrument.PROP_PRE_FLUSHING_TIME));
            lastZero = zero;
          }

          if (recordTime.isBefore(flushingEndTime)
            || (recordTime.isEqual(flushingEndTime) && instrument
              .getIntProperty(Instrument.PROP_PRE_FLUSHING_TIME) > 0)) {
            SensorValue co2 = recordValues.get(co2Column);
            co2.setUserQC(Flag.FLUSHING, "Flushing");
            flaggedSensorValues.add(co2);
          }

          if (runType.equals(WATER_MODE) && instrument.hasVariable(waterVar)) {
            measurements
              .add(new Measurement(dataset.getId(), recordTime, waterRunTypes));
          }

          if (runType.equals(ATM_MODE) && instrument.hasVariable(atmVar)) {
            measurements
              .add(new Measurement(dataset.getId(), recordTime, atmRunTypes));
          }
        }
      }

      DataSetDataDB.storeSensorValues(conn, flaggedSensorValues);
      return measurements;
    } catch (Exception e) {
      if (e instanceof MeasurementLocatorException) {
        throw (MeasurementLocatorException) e;
      } else {
        throw new MeasurementLocatorException(e);
      }
    }
  }
}
