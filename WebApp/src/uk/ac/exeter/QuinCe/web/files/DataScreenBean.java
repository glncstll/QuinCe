package uk.ac.exeter.QuinCe.web.files;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import uk.ac.exeter.QCRoutines.messages.Flag;
import uk.ac.exeter.QuinCe.data.FileInfo;
import uk.ac.exeter.QuinCe.data.Instrument;
import uk.ac.exeter.QuinCe.data.RunType;
import uk.ac.exeter.QuinCe.database.DatabaseException;
import uk.ac.exeter.QuinCe.database.RecordNotFoundException;
import uk.ac.exeter.QuinCe.database.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.database.QC.QCDB;
import uk.ac.exeter.QuinCe.database.files.DataFileDB;
import uk.ac.exeter.QuinCe.database.files.FileDataInterrogator;
import uk.ac.exeter.QuinCe.jobs.JobManager;
import uk.ac.exeter.QuinCe.jobs.files.FileJob;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.StringUtils;
import uk.ac.exeter.QuinCe.web.BaseManagedBean;
import uk.ac.exeter.QuinCe.web.system.ResourceException;
import uk.ac.exeter.QuinCe.web.system.ServletUtils;

public class DataScreenBean extends BaseManagedBean {

	public static final String PAGE_START = "data_screen";
	
	public static final String PAGE_END = "file_list";
	
	private static final String POPUP_PLOT = "plot";
	
	private static final String POPUP_MAP = "map";
	
	private long fileId;
	
	private FileInfo fileDetails = null;
	
	private String leftPlotColumns = null;
	
	private String leftPlotData = null;
	
	private String leftPlotNames = null;
	
	private String rightPlotColumns = null;
	
	private String rightPlotData = null;
	
	private String rightPlotNames = null;
	
	private int co2Type = RunType.RUN_TYPE_WATER;
	
	private List<String> optionalFlags = null;
	
	private String tableMode = "basic";
	
	private String tableJsonData = null;

	private int tableDataDraw;		

	private int tableDataStart;		

	private int tableDataLength;		

	private int recordCount = -1;	

	private String selectedRows = null;
	
	private String woceComment = null;
	
	private int woceFlag = Flag.VALUE_NEEDED;
	
	private Instrument instrument;

	private boolean dirty = false;
	
	/**
	 * Required basic constructor. All the actual construction
	 * is done in start().
	 */
	public DataScreenBean() {
		// Do nothing
	}

	public String start() throws Exception {
		clearData();
		loadFileDetails();
		
		// Temporarily always show Bad flags
		List<String> badFlags = new ArrayList<String>(1);
		badFlags.add("4");
		setOptionalFlags(badFlags);
		
		return PAGE_START;
	}
	
	public String end() throws Exception {
		
		if (dirty) {
			Map<String, String> parameters = new HashMap<String, String>(1);
			parameters.put(FileJob.FILE_ID_KEY, String.valueOf(fileId));
			
			DataSource dataSource = ServletUtils.getDBDataSource();
			Connection conn = dataSource.getConnection();
			
			JobManager.addJob(conn, getUser(), FileInfo.getJobClass(FileInfo.JOB_CODE_REDUCTION), parameters);
			DataFileDB.setCurrentJob(conn, fileId, FileInfo.JOB_CODE_REDUCTION);
		}
		
		clearData();
		return PAGE_END;
	}
	
	private void clearData() {
		fileDetails = null;
		leftPlotColumns = null;
		leftPlotData = null;
		rightPlotColumns = null;
		rightPlotData = null;
		optionalFlags = null;
		tableJsonData = null;
		recordCount = -1;
		dirty = false;
	}
	
	public long getFileId() {
		return fileId;
	}
	
	public void setFileId(long fileId) {
		this.fileId = fileId;
	}
	
	public FileInfo getFileDetails() {
		return fileDetails;
	}
	
	public String getLeftPlotColumns() {
		return leftPlotColumns;
	}
	
	public void setLeftPlotColumns(String leftPlotColumns) {
		this.leftPlotColumns = leftPlotColumns;
	}
	
	public String getLeftPlotData() {
		return leftPlotData;
	}
	
	public void setLeftPlotData(String leftPlotData) {
		this.leftPlotData = leftPlotData;
	}
	
	public String getLeftPlotNames() {
		return leftPlotNames;
	}
	
	public void setLeftPlotNames(String leftPlotNames){
		this.leftPlotNames = leftPlotNames;
	}
	
	public String getRightPlotColumns() {
		return rightPlotColumns;
	}
	
	public void setRightPlotColumns(String rightPlotColumns) {
		this.rightPlotColumns = rightPlotColumns;
	}
	
	public String getRightPlotData() {
		return rightPlotData;
	}
	
	public void setRightPlotData(String rightPlotData) {
		this.rightPlotData = rightPlotData;
	}
	
	public String getRightPlotNames() {
		return rightPlotNames;
	}
	
	public void setRightPlotNames(String rightPlotNames){
		this.rightPlotNames = rightPlotNames;
	}
	
	public int getCo2Type() {
		return co2Type;
	}
	
	public void setCo2Type(int co2Type) {
		this.co2Type = co2Type;
	}
	
	public List<String> getOptionalFlags() {
		return optionalFlags;
	}
	
	public void setOptionalFlags(List<String> optionalFlags) {
		if (optionalFlags.contains(String.valueOf(Flag.VALUE_BAD)) && !optionalFlags.contains(String.valueOf(Flag.VALUE_FATAL))) {
			optionalFlags.add(String.valueOf(Flag.VALUE_FATAL));
		}
		
		this.optionalFlags = optionalFlags;
		
		// Reset the record count, so it is retrieved from the database again.		
		recordCount = -1;
	}
	
	public String getTableMode() {
		return tableMode;
	}
	
	public void setTableMode(String tableMode) {
		this.tableMode = tableMode;
	}
	
	public String getTableJsonData() {
 		return tableJsonData;		
 	}
	
 	public void setTableJsonData(String tableJsonData) {
 		this.tableJsonData = tableJsonData;
 	}		
 
 	public int getTableDataDraw() {		
		return tableDataDraw;		
	}		
			
	public void setTableDataDraw(int tableDataDraw) {		
		this.tableDataDraw = tableDataDraw;		
	}		
			
	public int getTableDataStart() {		
		return tableDataStart;		
	}		
			
	public void setTableDataStart(int tableDataStart) {		
		this.tableDataStart = tableDataStart;		
	}		
			
	public int getTableDataLength() {		
		return tableDataLength;		
	}		
			
	public void setTableDataLength(int tableDataLength) {		
		this.tableDataLength = tableDataLength;		
	}		
			
	public int getRecordCount() {		
		return recordCount;		
	}
  			  	
	public void setRecordCount(int recordCount) {
		this.recordCount = recordCount;
  	}
 
	public String getSelectedRows() {
		return selectedRows;
	}
	
	public void setSelectedRows(String selectedRows) {
		this.selectedRows = selectedRows;
	}
	
	public String getWoceComment() {
		return woceComment;
	}
	
	public void setWoceComment(String woceComment) {
		this.woceComment = woceComment;
	}
	
	public int getWoceFlag() {
		return woceFlag;
	}
	
	public void setWoceFlag(int woceFlag) {
		this.woceFlag = woceFlag;
	}
	
	private void loadFileDetails() throws MissingParamException, DatabaseException, ResourceException, RecordNotFoundException {
		fileDetails = DataFileDB.getFileDetails(ServletUtils.getDBDataSource(), fileId);
		DataFileDB.touchFile(ServletUtils.getDBDataSource(), fileId);
		instrument = InstrumentDB.getInstrumentByFileId(ServletUtils.getDBDataSource(), fileId);
	}
	
	public String getPlotPopupEntries() throws MissingParamException, DatabaseException, RecordNotFoundException, ResourceException {
		
		Instrument instrument = InstrumentDB.getInstrument(ServletUtils.getDBDataSource(), fileDetails.getInstrumentId());
		
		StringBuffer output = new StringBuffer();
		
		output.append("<table><tr>");
		
		// First column
		output.append("<td><table>");
		
		output.append(makePlotCheckbox("datetime", "dateTime", "Date/Time"));
		output.append(makePlotCheckbox("longitude", "longitude", "Longitude"));
		output.append(makePlotCheckbox("latitude", "latitude", "Latitude"));

		// Intake temperature
		if (instrument.getIntakeTempCount() == 1) {
			output.append(makePlotCheckbox("intakeTemp", "intakeTempMean", "Intake Temperature"));
		} else {
			output.append("<tr><td colspan=\"2\" class=\"minorHeading\">Intake Temperature:</td></tr>");
			output.append("<tr><td></td><td><table>");

			output.append(makePlotCheckbox("intakeTemp", "intakeTempMean", "Mean"));
			
			if (instrument.hasIntakeTemp1()) {
				output.append(makePlotCheckbox("intakeTemp", "intakeTemp1", instrument.getIntakeTempName1()));
			}
			
			if (instrument.hasIntakeTemp2()) {
				output.append(makePlotCheckbox("intakeTemp", "intakeTemp2", instrument.getIntakeTempName2()));
			}
			
			if (instrument.hasIntakeTemp3()) {
				output.append(makePlotCheckbox("intakeTemp", "intakeTemp3", instrument.getIntakeTempName3()));
			}
			
			output.append("</table></td></tr>");
		}

		// Salinity
		if (instrument.getSalinityCount() == 1) {
			output.append(makePlotCheckbox("salinity", "salinityMean", "Salinity"));
		} else {
			output.append("<tr><td colspan=\"2\" class=\"minorHeading\">Salinity:</td></tr>");
			output.append("<tr><td></td><td><table>");

			output.append(makePlotCheckbox("salinity", "salinityMean", "Mean"));
			
			if (instrument.hasSalinity1()) {
				output.append(makePlotCheckbox("salinity", "salinity1", instrument.getSalinityName1()));
			}
			
			if (instrument.hasSalinity2()) {
				output.append(makePlotCheckbox("salinity", "salinity2", instrument.getSalinityName2()));
			}
			
			if (instrument.hasSalinity3()) {
				output.append(makePlotCheckbox("salinity", "salinity3", instrument.getSalinityName3()));
			}

			output.append("</table></td></tr>");
		}
		
		// End of first column/start of second
		output.append("</table></td><td><table>");
		
		boolean flowSensor = false;
		
		if (instrument.getAirFlowCount() > 0) {
			flowSensor = true;
			
			output.append("<tr><td colspan=\"2\" class=\"minorHeading\">Air Flow:</td></tr>");
			output.append("<tr><td></td><td><table>");
			
			if (instrument.hasAirFlow1()) {
				output.append(makePlotCheckbox("airFlow", "airFlow1", instrument.getAirFlowName1()));
			}
			
			if (instrument.hasAirFlow2()) {
				output.append(makePlotCheckbox("airFlow", "airFlow2", instrument.getAirFlowName2()));
			}
			
			if (instrument.hasAirFlow3()) {
				output.append(makePlotCheckbox("airFlow", "airFlow3", instrument.getAirFlowName3()));
			}
			
			output.append("</table></td></tr>");
		}
		
		if (instrument.getWaterFlowCount() > 0) {
			flowSensor = true;
			
			output.append("<tr><td colspan=\"2\" class=\"minorHeading\">Water Flow:</td></tr>");
			output.append("<tr><td></td><td><table>");
			
			if (instrument.hasWaterFlow1()) {
				output.append(makePlotCheckbox("waterFlow", "waterFlow1", instrument.getWaterFlowName1()));
			}
			
			if (instrument.hasWaterFlow2()) {
				output.append(makePlotCheckbox("waterFlow", "waterFlow2", instrument.getWaterFlowName2()));
			}
			
			if (instrument.hasWaterFlow3()) {
				output.append(makePlotCheckbox("waterFlow", "waterFlow3", instrument.getWaterFlowName3()));
			}
			
			output.append("</table></td></tr>");
		}
		
		if (flowSensor) {
			// End of 2nd column/start of 3rd
			output.append("</table></td><td><table>");
		}

		// Equilibrator temperature
		if (instrument.getEqtCount() == 1) {
			output.append(makePlotCheckbox("eqt", "eqtMean", "Equilibrator Temperature"));
		} else {
			output.append("<tr><td colspan=\"2\" class=\"minorHeading\">Equilibrator Temperature:</td></tr>");
			output.append("<tr><td></td><td><table>");
			
			output.append(makePlotCheckbox("eqt", "eqtMean", "Mean"));
			
			if (instrument.hasEqt1()) {
				output.append(makePlotCheckbox("eqt", "eqt1", instrument.getEqtName1()));
			}
			
			if (instrument.hasEqt2()) {
				output.append(makePlotCheckbox("eqt", "eqt2", instrument.getEqtName2()));
			}
			
			if (instrument.hasEqt3()) {
				output.append(makePlotCheckbox("eqt", "eqt3", instrument.getEqtName3()));
			}
			
			output.append("</table></td></tr>");
		}
		
		// Delta T
		output.append(makePlotCheckbox("deltaT", "deltaT", "Δ Temperature"));

		// Equilibrator Pressure
		if (instrument.getEqpCount() == 1) {
			output.append(makePlotCheckbox("eqp", "eqpMean", "Equilibrator Pressure"));
		} else {
			output.append("<tr><td colspan=\"2\" class=\"minorHeading\">Equilibrator Pressure:</td></tr>");
			output.append("<tr><td></td><td><table>");

			output.append(makePlotCheckbox("eqp", "eqpMean", "Mean"));
			
			if (instrument.hasEqp1()) {
				output.append(makePlotCheckbox("eqp", "eqp1", instrument.getEqpName1()));
			}
			
			if (instrument.hasEqp2()) {
				output.append(makePlotCheckbox("eqp", "eqp2", instrument.getEqpName2()));
			}
			
			if (instrument.hasEqp3()) {
				output.append(makePlotCheckbox("eqp", "eqp3", instrument.getEqpName3()));
			}

			output.append("</table></td></tr>");
		}
		
		// Atmospheric Pressure
		/*
		 * We'll put this in when we get to doing atmospheric stuff.
		 * It needs to specify whether it's measured or from external data
		 * 
		output.append(makePlotCheckbox("atmosPressure", "atmospressure", "Atmospheric Pressure"));
		output.append("</td><td>Atmospheric Pressure</td></tr>");
		*/
		
		// xH2O
		output.append("<tr><td colspan=\"2\" class=\"minorHeading\">xH<sub>2</sub>O:</td></tr>");
		output.append("<tr><td></td><td><table>");

		output.append(makePlotCheckbox("xh2o", "xh2oMeasured", "Measured"));
		output.append(makePlotCheckbox("xh2o", "xh2oTrue", "True"));
		
		output.append("</table></td></tr>");

		// pH2O
		output.append(makePlotCheckbox("pH2O", "pH2O", "pH<sub>2</sub>O"));

		// End of 3rd column/Start of 4th column
		output.append("</table></td><td><table>");

		// CO2
		output.append("<tr><td colspan=\"2\" class=\"minorHeading\">CO<sub>2</sub>:</td></tr>");
		output.append("<tr><td></td><td><table>");

		output.append(makePlotCheckbox("co2", "co2Measured", "Measured"));

		if (!instrument.getSamplesDried()) {
			output.append(makePlotCheckbox("co2", "co2Dried", "Dried"));
		}

		output.append(makePlotCheckbox("co2", "co2Calibrated", "Calibrated"));
		output.append(makePlotCheckbox("co2", "pCO2TEDry", "pCO<sub>2</sub> TE Dry"));
		output.append(makePlotCheckbox("co2", "pCO2TEWet", "pCO<sub>2</sub> TE Wet"));
		output.append(makePlotCheckbox("co2", "fCO2TE", "fCO<sub>2</sub> TE"));
		output.append(makePlotCheckbox("co2", "fCO2Final", "fCO<sub>2</sub> Final"));

		output.append("</table></td></tr>");

		// End of column 4
		output.append("</td></table>");
		
		// End of outer table
		output.append("</tr></table>");
		
		return output.toString();
	}
	
	private String makePlotCheckbox(String group, String field, String label) {
		return makeCheckbox(POPUP_PLOT, group, field, label);
	}
	
	private String makeMapCheckbox(String group, String field, String label) {
		return makeCheckbox(POPUP_MAP, group, field, label);
	}
	
	private String makeCheckbox(String popupType, String group, String field, String label) {

		String inputID = popupType + "_" + group + "_" + field;
		
		StringBuffer checkbox = new StringBuffer();
		checkbox.append("<tr><td><input type=\"checkbox\" id=\"");
		checkbox.append(inputID);
		checkbox.append("\" value=\"");
		checkbox.append(field);
		checkbox.append("\"/></td><td><label for=\"");
		checkbox.append(inputID);
		checkbox.append("\">");
		checkbox.append(label);
		checkbox.append("</label></td></tr>");
		
		return checkbox.toString();
	}
	
	public void generateLeftPlotData() {
		List<String> columns = StringUtils.delimitedToList(leftPlotColumns);
		setLeftPlotData(getPlotData(columns));
		setLeftPlotNames(makePlotNames(columns));
	}

	public void generateRightPlotData() {
		List<String> columns = StringUtils.delimitedToList(rightPlotColumns);
		setRightPlotData(getPlotData(columns)); 
		setRightPlotNames(makePlotNames(columns));
	}
	
	private String getPlotData(List<String> columns) {
		
		String output;
		
		try {
			DataSource dataSource = ServletUtils.getDBDataSource();
			
			// Add in the row number and flags as the first Y-axis columns. We need it for syncing the graphs and the table
			// The list returned from delimitedToList does not allow inserting, so we have to do it the hard way.
			List<String> submittedColumnList = new ArrayList<String>(columns.size() + 1);
			
			// Add the X axis
			submittedColumnList.add(columns.get(0));
			
			// Now the row number
			submittedColumnList.add("row");
			
			// Add QC and WOCE flags
			submittedColumnList.add("qcFlag");
			submittedColumnList.add("woceFlag");
			
			// And the Y axis columns
			submittedColumnList.addAll(columns.subList(1, columns.size()));
			
			output = FileDataInterrogator.getJsonDataArray(dataSource, fileId, co2Type, submittedColumnList, getIncludeFlags(), 1, 0, true, false);
		} catch (Exception e) {
			e.printStackTrace();
			output = "***ERROR: " + e.getMessage();
		}
		
		return output;
	}

	public void generateTableData() {

		try {
			DataSource dataSource = ServletUtils.getDBDataSource();
			
			if (recordCount < 0) {		
				setRecordCount(FileDataInterrogator.getRecordCount(dataSource, fileId, co2Type, getIncludeFlags()));
			}
			
			List<String> columns = new ArrayList<String>();
			columns.add("dateTime");
			columns.add("row");
			columns.add("longitude");
			columns.add("latitude");
			
			if (instrument.getIntakeTempCount() == 1) {
				columns.add("intakeTempMean");
			} else {
				if (instrument.hasIntakeTemp1()) {
					columns.add("intakeTemp1");
				}
				if (instrument.hasIntakeTemp2()) {
					columns.add("intakeTemp2");
				}
				if (instrument.hasIntakeTemp3()) {
					columns.add("intakeTemp3");
				}
				
				columns.add("intakeTempMean");
			}
			
			if (instrument.getSalinityCount() == 1) {
				columns.add("salinityMean");
			} else {
				if (instrument.hasSalinity1()) {
					columns.add("salinity1");
				}
				if (instrument.hasSalinity2()) {
					columns.add("salinity2");
				}
				if (instrument.hasSalinity3()) {
					columns.add("salinity3");
				}
				
				columns.add("salinityMean");
			}
			
			if (instrument.hasAirFlow1()) {
				columns.add("air_flow_1");
			}
			if (instrument.hasAirFlow2()) {
				columns.add("air_flow_2");
			}
			if (instrument.hasAirFlow3()) {
				columns.add("air_flow_3");
			}

			if (instrument.hasWaterFlow1()) {
				columns.add("water_flow_1");
			}
			if (instrument.hasWaterFlow2()) {
				columns.add("water_flow_2");
			}
			if (instrument.hasWaterFlow3()) {
				columns.add("water_flow_3");
			}
			
			if (instrument.getEqtCount() == 1) {
				columns.add("eqtMean");
			} else {
				if (instrument.hasEqt1()) {
					columns.add("eqt1");
				}
				if (instrument.hasEqt2()) {
					columns.add("eqt2");
				}
				if (instrument.hasEqt3()) {
					columns.add("eqt3");
				}
				
				columns.add("eqtMean");
			}
			
			columns.add("deltaT");
			
			if (instrument.getEqpCount() == 1) {
				columns.add("eqpMean");
			} else {
				if (instrument.hasEqp1()) {
					columns.add("eqp1");
				}
				if (instrument.hasEqp2()) {
					columns.add("eqp2");
				}
				if (instrument.hasEqp3()) {
					columns.add("eqp3");
				}
				
				columns.add("eqtMean");
			}
			
			columns.add("atmosPressure");
			columns.add("xh2oMeasured");
			columns.add("xh2oTrue");
			columns.add("pH2O");
			columns.add("co2Measured");
			columns.add("co2Dried");
			columns.add("co2Calibrated");
			columns.add("pCO2TEDry");
			columns.add("pCO2TEWet");
			columns.add("fCO2TE");
			columns.add("fCO2Final");
			columns.add("qcFlag");
			columns.add("qcMessage");
			columns.add("woceFlag");
			columns.add("woceMessage");
			
			setTableJsonData(FileDataInterrogator.getJsonDataObjects(dataSource, fileId, co2Type, columns, getIncludeFlags(), tableDataStart, tableDataLength, true, true, true));
		} catch (Exception e) {
			e.printStackTrace();
			setTableJsonData("***ERROR: " + e.getMessage());
		}
	}
	
	public String getTableHeadings() {

		StringBuffer output = new StringBuffer('[');
		
		output.append("['Date/Time', 'Row', 'Longitude', 'Latitude', ");
			
		if (instrument.getIntakeTempCount() == 1) {
			output.append("'Intake Temp', ");
		} else {
			if (instrument.hasIntakeTemp1()) {
				output.append("'Intake Temp:<br/>");
				output.append(instrument.getIntakeTempName1());
				output.append("', ");
			}
			if (instrument.hasIntakeTemp2()) {
				output.append("'Intake Temp:<br/>");
				output.append(instrument.getIntakeTempName2());
				output.append("', ");
			}
			if (instrument.hasIntakeTemp3()) {
				output.append("'Intake Temp:<br/>");
				output.append(instrument.getIntakeTempName3());
				output.append("', ");
			}
			
			output.append("'Intake Temp:<br/>Mean', ");
		}
			
		if (instrument.getSalinityCount() == 1) {
			output.append("'Salinity', ");
		} else {
			if (instrument.hasSalinity1()) {
				output.append("'Salinity:<br/>");
				output.append(instrument.getSalinityName1());
				output.append("', ");
			}
			if (instrument.hasSalinity2()) {
				output.append("'Salinity:<br/>");
				output.append(instrument.getSalinityName2());
				output.append("', ");
			}
			if (instrument.hasSalinity3()) {
				output.append("'Salinity:<br/>");
				output.append(instrument.getSalinityName3());
				output.append("', ");
			}
			
			output.append("'Salinity:<br/>Mean', ");
		}
		
		if (instrument.hasAirFlow1()) {
			output.append("'Air Flow:<br/>");
			output.append(instrument.getAirFlowName1());
			output.append("', ");
		}
		if (instrument.hasAirFlow2()) {
			output.append("'Air Flow:<br/>");
			output.append(instrument.getAirFlowName2());
			output.append("', ");
		}
		if (instrument.hasAirFlow3()) {
			output.append("'Air Flow:<br/>");
			output.append(instrument.getAirFlowName3());
			output.append("', ");
		}

		if (instrument.hasWaterFlow1()) {
			output.append("'Water Flow:<br/>");
			output.append(instrument.getWaterFlowName1());
			output.append("', ");
		}
		if (instrument.hasWaterFlow2()) {
			output.append("'Water Flow:<br/>");
			output.append(instrument.getWaterFlowName2());
			output.append("', ");
		}
		if (instrument.hasWaterFlow3()) {
			output.append("'Water Flow:<br/>");
			output.append(instrument.getWaterFlowName3());
			output.append("', ");
		}

		if (instrument.getEqtCount() == 1) {
			output.append("'Equil. Temp', ");
		} else {
			if (instrument.hasEqt1()) {
				output.append("'Equil. Temp:<br/>");
				output.append(instrument.getEqtName1());
				output.append("', ");
			}
			if (instrument.hasEqt2()) {
				output.append("'Equil. Temp:<br/>");
				output.append(instrument.getEqtName2());
				output.append("', ");
			}
			if (instrument.hasEqt3()) {
				output.append("'Equil. Temp:<br/>");
				output.append(instrument.getEqtName3());
				output.append("', ");
			}
			
			output.append("'Equil. Temp:<br/>Mean', ");
		}
		
		output.append("'Δ Temperature', ");

		if (instrument.getEqpCount() == 1) {
			output.append("'Equil. Pressure', ");
		} else {
			if (instrument.hasEqp1()) {
				output.append("'Equil. Pressure:<br/>");
				output.append(instrument.getEqpName1());
				output.append("', ");
			}
			if (instrument.hasEqp2()) {
				output.append("'Equil. Pressure:<br/>");
				output.append(instrument.getEqpName2());
				output.append("', ");
			}
			if (instrument.hasEqp3()) {
				output.append("'Equil. Pressure:<br/>");
				output.append(instrument.getEqpName3());
				output.append("', ");
			}
			
			output.append("'Equil. Pressure:<br/>Mean', ");
		}

		output.append("'Atmos. Pressure', 'xH₂O (Measured)', 'xH₂O (True)', 'pH₂O', 'CO₂ Measured', 'CO₂ Dried', 'CO₂ Calibrated', 'pCO₂ TE Dry', "
				+ "'pCO₂ TE Wet', 'fCO₂ TE', 'fCO₂ Final', 'QC Flag', 'QC Message', 'WOCE Flag', 'WOCE Message']");
		
		return output.toString();
	}

	private List<Integer> getIncludeFlags() {
		List<Integer> includeFlags = new ArrayList<Integer>();
		includeFlags.add(Flag.VALUE_GOOD);
		includeFlags.add(Flag.VALUE_ASSUMED_GOOD);
		includeFlags.add(Flag.VALUE_QUESTIONABLE);
		includeFlags.add(Flag.VALUE_NEEDED);
		
		if (null != optionalFlags) {
			for (String optionalFlag : optionalFlags) {
				includeFlags.add(Integer.parseInt(optionalFlag));
			}
		}
		
		return includeFlags;
	}
	
	public Instrument getInstrument() {
		return instrument;
	}
	
	public void acceptQCFlags() {
		try {
			QCDB.acceptQCFlags(ServletUtils.getDBDataSource(), fileId, getSelectedRows());
			dirty = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void applyWoceFlag() {
		try {
			QCDB.setWoceFlags(ServletUtils.getDBDataSource(), fileId, getSelectedRows(), getWoceFlag(), getWoceComment());
			dirty = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected String getFormName() {
		return "dataScreen";
	}
	
	/**
	 * 
	 * @param columns
	 * @return
	 * @see #getPlotData(List)
	 */
	private String makePlotNames(List<String> columns) {

		List<String> output = new ArrayList<String>(columns.size());
		
		// The first column is the X axis
		output.add(getPlotSeriesName(columns.get(0)));
		
		// Next are the row, QC Flag and WOCE Flag. These are fixed internal series
		// That are never displayed.
		output.add("Row");
		output.add("QC Flag");
		output.add("WOCE Flag");
		
		// Now the rest of the columns
		for (int i = 1; i < columns.size(); i++) {
			output.add(getPlotSeriesName(columns.get(i)));
		}
		
		return StringUtils.listToDelimited(output);
	}


	private String getPlotSeriesName(String series) {
		
		String result;
		
		switch (series) {
		case "dateTime": {
			result = ("Date/Time");
			break;
		}
		case("longitude"): {
			result = ("Longitude");
			break;
		}
		case("latitude"): {
			result = ("Latitude");
			break;
		}
		case("intakeTemp1"): {
			result = (instrument.getIntakeTempName1());
			break;
		}
		case("intakeTemp2"): {
			result = (instrument.getIntakeTempName2());
			break;
		}
		case("intakeTemp3"): {
			result = (instrument.getIntakeTempName3());
			break;
		}
		case("intakeTempMean"): {
			result = ("Mean Intake Temp");
			break;
		}
		case("salinity1"): {
			result = (instrument.getSalinityName1());
			break;
		}
		case("salinity2"): {
			result = (instrument.getSalinityName2());
			break;
		}
		case("salinity3"): {
			result = (instrument.getSalinityName3());
			break;
		}
		case("salinityMean"): {
			result = ("Mean Salinity");
			break;
		}
		case("eqt1"): {
			result = (instrument.getEqtName1());
			break;
		}
		case("eqt2"): {
			result = (instrument.getEqtName2());
			break;
		}
		case("eqt3"): {
			result = (instrument.getEqtName3());
			break;
		}
		case("eqtMean"): {
			result = ("Mean Equil Temp");
			break;
		}
		case("deltaT"): {
			result = ("Δ Temp");
			break;
		}
		case("eqp1"): {
			result = (instrument.getEqpName1());
			break;
		}
		case("eqp2"): {
			result = (instrument.getEqpName2());
			break;
		}
		case("eqp3"): {
			result = (instrument.getEqpName3());
			break;
		}
		case("eqpMean"): {
			result = ("Mean Equil Pres");
			break;
		}
		case("airFlow1"): {
			result = (instrument.getAirFlowName1());
			break;
		}
		case("airFlow2"): {
			result = (instrument.getAirFlowName2());
			break;
		}
		case("airFlow3"): {
			result = (instrument.getAirFlowName3());
			break;
		}
		case("waterFlow1"): {
			result = (instrument.getWaterFlowName1());
			break;
		}
		case("waterFlow2"): {
			result = (instrument.getWaterFlowName2());
			break;
		}
		case("waterFlow3"): {
			result = (instrument.getWaterFlowName3());
			break;
		}
		case("moistureMeasured"): {
			result = ("Moisture (Measured)");
			break;
		}
		case("moistureTrue"): {
			result = ("Moisture (True)");
			break;
		}
		case("pH2O"): {
			result = ("pH₂O");
			break;
		}
		case("co2Measured"): {
			result = ("Measured CO₂");
			break;
		}
		case("co2Dried"): {
			result = ("Dried CO₂");
			break;
		}
		case("co2Calibrated"): {
			result = ("Calibrated CO₂");
			break;
		}
		case("pCO2TEDry"): {
			result = ("pCO₂ TE Dry");
			break;
		}
		case("pCO2TEWet"): {
			result = ("pCO₂ TE Wet");
			break;
		}
		case("fCO2TE"): {
			result = ("fCO₂ TE");
			break;
		}
		case("fCO2Final"): {
			result = ("Final fCO₂");
			break;
		}
		default: {
			result = ("***UNKNOWN COLUMN " + series + "***");
		}
		}

		return result;
	}
}