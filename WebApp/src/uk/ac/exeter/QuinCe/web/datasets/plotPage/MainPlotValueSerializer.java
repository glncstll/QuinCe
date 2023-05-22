package uk.ac.exeter.QuinCe.web.datasets.plotPage;

import java.lang.reflect.Type;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import uk.ac.exeter.QuinCe.utils.DateTimeUtils;

public class MainPlotValueSerializer implements JsonSerializer<PlotValue> {

  private boolean plotHasY2;

  protected MainPlotValueSerializer(boolean plotHasY2) {
    this.plotHasY2 = plotHasY2;
  }

  @Override
  public JsonElement serialize(PlotValue src, Type typeOfSrc,
    JsonSerializationContext context) {

    JsonArray json = new JsonArray();

    if (src.xIsTime()) {
      json.add(DateTimeUtils.toIsoDate(src.getXTime()));
    } else {
      Double x = src.getXDouble();
      if (null == x || x.isNaN()) {
        json.add(JsonNull.INSTANCE);
      } else {
        json.add(src.getXDouble());
      }
    }

    json.add(src.getId());

    if (!src.hasY()) {
      json.add(JsonNull.INSTANCE);
      json.add(JsonNull.INSTANCE);
    } else if (src.isGhost()) {
      json.add(src.getY());
      json.add(JsonNull.INSTANCE);
    } else {
      json.add(JsonNull.INSTANCE);
      json.add(src.getY());
    }

    if (plotHasY2) {
      json.add(JsonNull.INSTANCE);
    }

    return json;
  }
}
