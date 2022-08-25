package com.mx.common.serialization;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Builder;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.mx.common.lang.Strings;

/**
 * LocalDate deserializer for use with Gson
 * <p>
 *   Default Behavior:
 *   <ul>
 *     <li>Serialize LocalDate to Object</li>
 *     <li>Allow deserialization of LocalDate from object Object</li>
 *     <li>Allow deserialization of LocalDate from from string format "yyyy-MM-dd"</li>
 *   </ul>
 *   <br/>
 *
 * </p>
 *   Examples:
 *
 * <pre>
 *   {@code
 *   // Take default behavior
 *   Gson gson = new GsonBuilder().registerTypeAdapter(
 *     LocalDate.class,
 *     LocalDateDeserializer.builder().build()
 *   ).create();
 *
 *   // Add acceptable deserialization formats (will still handle object)
 *   // Removes default format. Needs to be added using .format() if still needed.
 *   Gson gson = new GsonBuilder().registerTypeAdapter(
 *     LocalDate.class,
 *     LocalDateDeserializer.builder()
 *       .format("yyyy-MM-dd")
 *       .build()
 *   ).create();
 *
 *   // Serialize as String
 *   // Must add it to accepted formats using .format in order to deserialize.
 *   Gson gson = new GsonBuilder().registerTypeAdapter(
 *     LocalDate.class,
 *     LocalDateDeserializer.builder()
 *       .serializeFormat("yyyy-MM-dd")
 *       .format("yyyy-MM-dd")
 *       .build()
 *   ).create();
 *   }
 * </pre>
 *
 */
@Builder
public class LocalDateDeserializer implements JsonDeserializer<LocalDate>, JsonSerializer<LocalDate> {

  public static final String DEFAULT_FORMAT = "yyyy-MM-dd";

  private static final List<DateTimeFormatter> DEFAULT_FORMAT_STRINGS;
  static {
    List<DateTimeFormatter> dateTimeFormatters = new ArrayList<>();
    dateTimeFormatters.add(DateTimeFormatter.ofPattern(DEFAULT_FORMAT));

    DEFAULT_FORMAT_STRINGS = Collections.unmodifiableList(dateTimeFormatters);
  }

  private static final Gson GSON = new Gson();

  private List<DateTimeFormatter> formats;

  private String serializeFormat;

  public static class LocalDateDeserializerBuilder {

    private List<DateTimeFormatter> formats = new ArrayList<>();

    private String serializeFormat = "OBJECT";

    /**
     * Provide a LocalDate string format that is accepted. (default: "yyyy-MM-dd")
     * <p>
     *   The formats need to be provided according to {@link DateTimeFormatter} specs.
     * </p>
     * <p>
     *   Providing any formats will remove the default format.
     * </p>
     * @param format Format string of acceptable LocalDate format
     * @return
     */
    public final LocalDateDeserializerBuilder format(String format) {
      formats.add(DateTimeFormatter.ofPattern(format));
      return this;
    }

    /**
     * Provide format for serializing a LocalDate object to JSON
     * @param format A DateTimeFormatter format String or
     *               OBJECT to serialize as an object (default)
     * @return builder
     */
    public final LocalDateDeserializer.LocalDateDeserializerBuilder serializeFormat(String format) {
      serializeFormat = format;
      return this;
    }
  }

  @Override
  public final LocalDate deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context)
      throws JsonParseException {

    if (json.isJsonObject()) {
      LocalDateJava8 localDateJava8 = GSON.fromJson(json, LocalDateJava8.class);
      return localDateJava8.toLocalDate();
    }

    JsonPrimitive dateJson = json.getAsJsonPrimitive();
    if (dateJson.isJsonNull()) {
      return null;
    }
    String localDateStr = dateJson.getAsString();
    for (DateTimeFormatter s : formats.isEmpty() ? DEFAULT_FORMAT_STRINGS : formats) {
      try {
        return LocalDate.parse(localDateStr, s);
      } catch (DateTimeParseException e) {
        continue;
      }
    }

    throw new JsonParseException("Invalid date: " + localDateStr);
  }

  @Override
  public final JsonElement serialize(LocalDate src, Type typeOfSrc, JsonSerializationContext context) {
    if (Strings.isBlank(serializeFormat) || serializeFormat.equals("OBJECT")) {
      return GSON.toJsonTree(new LocalDateJava8(src));
    } else {
      return new JsonPrimitive(src.format(DateTimeFormatter.ofPattern(serializeFormat)));
    }
  }
}
