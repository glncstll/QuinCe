package uk.ac.exeter.QuinCe.web.datasets.plotPage;

import java.lang.reflect.Type;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;

public class PlotPageTableRecordSerializer
  implements JsonSerializer<PlotPageTableRecord> {

  private Gson gson;

  public PlotPageTableRecordSerializer(DatasetSensorValues allSensorValues) {
    gson = new GsonBuilder()
      .registerTypeHierarchyAdapter(PlotPageTableValue.class,
        new PlotPageTableValueSerializer(allSensorValues))
      .create();
  }

  @Override
  public JsonElement serialize(PlotPageTableRecord src, Type typeOfSrc,
    JsonSerializationContext context) {

    JsonObject jsonMap = new JsonObject();
    jsonMap.addProperty(PlotPageTableRecord.ID_KEY, src.getId());

    for (Map.Entry<Integer, PlotPageTableValue> columnEntry : src.getColumns()
      .entrySet()) {
      jsonMap.add(String.valueOf(columnEntry.getKey()),
        gson.toJsonTree(columnEntry.getValue()));
    }

    // TODO Auto-generated method stub
    return jsonMap;
  }
}
