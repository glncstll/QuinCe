package uk.ac.exeter.QuinCe.api.nrt;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.Properties;

import javax.sql.DataSource;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Files.DataFileDB;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.jobs.JobManager;
import uk.ac.exeter.QuinCe.jobs.files.CreateNrtDataset;
import uk.ac.exeter.QuinCe.jobs.files.ExtractDataSetJob;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.ExceptionUtils;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * API Method to create NRT datasets
 *
 * @author Steve Jones
 *
 */
@Path("/nrt/MakeNrtDataset")
public class MakeNrtDataset {

  /**
   * Main API method. Performs checks then tries to create the NRT dataset.
   *
   * @param instrumentId
   *          The instrument ID ({@code instrument} parameter)
   * @return The response
   */
  @POST
  public Response makeNrtDataset(@FormParam("instrument") long instrumentId) {

    Response response;

    Connection conn = null;
    try {
      DataSource dataSource = ResourceManager.getInstance().getDBDataSource();
      conn = dataSource.getConnection();

      if (!InstrumentDB.instrumentExists(conn, instrumentId)) {
        response = Response.status(Status.NOT_FOUND).build();
      } else {
        Instrument instrument = InstrumentDB.getInstrument(conn, instrumentId);

        if (!instrument.getNrt()) {
          response = Response.status(Status.FORBIDDEN).build();
        } else {
          if (createNrtDataset(conn, instrument)) {
            response = Response.status(Status.OK).build();
          } else {
            response = Response.status(Status.NO_CONTENT).build();
          }
        }
      }
    } catch (Exception e) {
      response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
      ExceptionUtils.printStackTrace(e);
    } finally {
      DatabaseUtils.closeConnection(conn);
    }

    return response;
  }

  /**
   * Attempt to create a NRT dataset for an instrument.
   *
   * @param conn
   *          A database connection.
   * @param instrument
   *          The instrument.
   * @return {@code true} if a new NRT dataset is created; {@code false} if no
   *         dataset is created.
   * @throws Exception
   *           Any errors are propagated upward.
   */
  public static boolean createNrtDataset(Connection conn, Instrument instrument)
    throws Exception {

    boolean createDataset = false;

    // Only try to create a new NRT dataset if either (a) there are no existing
    // NRT datasets or (b) there is an NRT dataset and its status is either
    // WAITING FOR EXPORT or EXPORT COMPLETE - any other time the NRT is being
    // processed, so leave it alone.
    DataSet existingDataset = DataSetDB.getNrtDataSet(conn, instrument.getId());

    // If there is no NRT dataset, create one
    if (null == existingDataset) {
      createDataset = true;
    } else {

      if (existingDataset.getStatus() == DataSet.STATUS_DELETE) {
        createDataset = true;
      } else if (existingDataset.getStatus() == DataSet.STATUS_READY_FOR_EXPORT
        || existingDataset.getStatus() == DataSet.STATUS_EXPORT_COMPLETE) {

        // See if any data files have been uploaded/updated since the NRT
        // dataset was created. If so, recreate it.
        LocalDateTime lastFileModification = DataFileDB
          .getLastFileModification(conn, instrument.getId());

        if (null != lastFileModification
          && lastFileModification.isAfter(existingDataset.getCreatedDate())) {

          createDataset = true;
        }
      }
    }

    if (createDataset) {
      Properties jobProperties = new Properties();
      jobProperties.setProperty(ExtractDataSetJob.ID_PARAM,
        String.valueOf(instrument.getId()));

      JobManager.addJob(conn, instrument.getOwner(),
        CreateNrtDataset.class.getCanonicalName(), jobProperties);
    }

    return createDataset;
  }
}
