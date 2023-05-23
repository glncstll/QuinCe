package uk.ac.exeter.QuinCe.web.datasets.plotPage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

import org.apache.commons.lang3.NotImplementedException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import uk.ac.exeter.QuinCe.data.Dataset.GeoBounds;

@SuppressWarnings("serial")
public class MapRecords extends ArrayList<MapRecord> {

  private static final int DECIMATION_LIMIT = 1000;

  private static Gson valueGson;

  private static Gson flagGson;

  private static Gson flagNrtGson;

  private static Gson selectionGson;

  static {
    valueGson = new GsonBuilder().registerTypeHierarchyAdapter(MapRecord.class,
      new MapRecordJsonSerializer(MapRecordJsonSerializer.VALUE)).create();
    flagGson = new GsonBuilder().registerTypeHierarchyAdapter(MapRecord.class,
      new MapRecordJsonSerializer(MapRecordJsonSerializer.FLAG)).create();
    flagNrtGson = new GsonBuilder()
      .registerTypeHierarchyAdapter(MapRecord.class,
        new MapRecordJsonSerializer(MapRecordJsonSerializer.FLAG_IGNORE_NEEDED))
      .create();
    selectionGson = new GsonBuilder()
      .registerTypeHierarchyAdapter(MapRecord.class,
        new MapRecordJsonSerializer(MapRecordJsonSerializer.SELECTION))
      .create();
  }

  private boolean valueRangeCalculated = false;

  private Double min = Double.NaN;

  private Double max = Double.NaN;

  public MapRecords(int size) {
    super(size);
  }

  public String getDisplayJson(GeoBounds bounds, List<Long> selectedRows,
    boolean useNeededFlags, boolean hideNonGoodFlags) {

    Set<MapRecord> boundedRecords = new TreeSet<MapRecord>();

    MapRecord minLon = get(0);
    MapRecord maxLon = get(0);
    MapRecord minLat = get(0);
    MapRecord maxLat = get(0);

    // Find the records within the specified bounds, and also record the records
    // closest to the bound limits.
    for (MapRecord record : this) {

      if (!hideNonGoodFlags || record.isGood() || record.flagNeeded()) {
        if (bounds.inBounds(record.position)) {
          if (record.position.getLongitude() < minLon.position.getLongitude()) {
            minLon = record;
          } else if (record.position.getLongitude() > maxLon.position
            .getLongitude()) {
            maxLon = record;
          }

          if (record.position.getLatitude() < minLat.position.getLatitude()) {
            minLat = record;
          } else if (record.position.getLatitude() > maxLat.position
            .getLatitude()) {
            maxLat = record;
          }

          boundedRecords.add(record);
        }
      }
    }

    // Decimate the chosen records
    Set<MapRecord> decimated = new HashSet<MapRecord>();
    List<MapRecord> selected = new ArrayList<MapRecord>();

    if (boundedRecords.size() <= DECIMATION_LIMIT) {
      decimated.addAll(boundedRecords);
    } else {
      int nth = (int) Math.floor(boundedRecords.size() / DECIMATION_LIMIT);
      int count = 0;
      for (MapRecord record : boundedRecords) {
        count++;

        if (count % nth == 0 || !record.isGood()) {
          decimated.add(record);
        }

        if (selectedRows.contains(record.getRowId())) {
          selected.add(record);
        }
      }
    }

    decimated.add(minLon);
    decimated.add(maxLon);
    decimated.add(minLat);
    decimated.add(maxLat);

    List<MapRecord> data = new ArrayList<MapRecord>();
    Set<MapRecord> flags = new TreeSet<MapRecord>();
    List<MapRecord> selection = new ArrayList<MapRecord>();

    for (MapRecord record : decimated) {
      data.add(record);
      if (showAsFlag(record, useNeededFlags)) {
        flags.add(record);
      }
    }

    for (MapRecord record : selected) {
      selection.add(record);
      if (showAsFlag(record, useNeededFlags)) {
        flags.add(record);
      }
    }

    JsonArray json = new JsonArray();

    json.add(valueGson.toJsonTree(makeFeatureCollection(valueGson, data)));

    if (useNeededFlags) {
      json.add(flagGson.toJsonTree(makeFeatureCollection(flagGson, flags)));
    } else {
      json
        .add(flagNrtGson.toJsonTree(makeFeatureCollection(flagNrtGson, flags)));
    }

    json.add(selectionGson
      .toJsonTree(makeFeatureCollection(selectionGson, selection)));

    return json.toString();
  }

  private boolean showAsFlag(MapRecord record, boolean useNeededFlag) {
    return (useNeededFlag && record.flagNeeded()) || !record.isGood();
  }

  private JsonObject makeFeatureCollection(Gson gson,
    Collection<MapRecord> points) {
    JsonObject object = new JsonObject();
    object.addProperty("type", "FeatureCollection");
    object.add("features", gson.toJsonTree(points));
    return object;
  }

  private void resetRange() {
    valueRangeCalculated = false;
  }

  private void calculateValueRange() {
    min = Double.NaN;
    max = Double.NaN;

    forEach(r -> {
      Double value = r.getValue();

      if (!value.isNaN()) {
        if (min.isNaN()) {
          min = value;
          max = value;
        } else {
          if (r.getValue() < min) {
            min = r.getValue();
          }
          if (r.getValue() > max) {
            max = r.getValue();
          }
        }
      }
    });

    valueRangeCalculated = true;
  }

  public Double[] getValueRange() {
    if (!valueRangeCalculated) {
      calculateValueRange();
    }

    return new Double[] { min, max };
  }

  @Override
  public void add(int index, MapRecord record) {
    throw new NotImplementedException();
  }

  @Override
  public boolean add(MapRecord record) {
    resetRange();
    boolean result;

    // Don't add NaN values
    if (record.isNaN()) {
      result = false;
    } else {
      result = super.add(record);
    }

    return result;
  }

  @Override
  public boolean addAll(int index, Collection<? extends MapRecord> records) {
    throw new NotImplementedException();
  }

  @Override
  public boolean addAll(Collection<? extends MapRecord> records) {
    throw new NotImplementedException();
  }

  @Override
  public MapRecord remove(int index) {
    throw new NotImplementedException();
  }

  @Override
  public boolean remove(Object o) {
    throw new NotImplementedException();
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    throw new NotImplementedException();
  }

  @Override
  public boolean removeIf(Predicate<? super MapRecord> filter) {
    throw new NotImplementedException();
  }

  @Override
  protected void removeRange(int fromIndex, int toIndex) {
    throw new NotImplementedException();
  }
}
