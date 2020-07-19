package com.fivetran.crossref;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class encapsulating the attributes of an item returned from the Crossref
 * API works endpoint.
 */
@Getter
@Setter
@Slf4j
public class Work {
  private String DOI;
  private List<String> titles;
  private List<String> authors;
  private String publisher;
  private Long timestamp;

  /**
   * Convert the JSON documented returned by the API into a collection of Work
   * objects.
   *
   * @param jsonNode JSON document returned by API call.
   * @return A collection of Work objects encapsulating items returned by the
   * API call.
   * @throws IOException Thrown if an exception occurs during parsing.
   */
  public static List<Work> parseItems(JsonNode jsonNode)
      throws IOException {
    List<Work> items = new ArrayList<>();
    for (JsonNode item : jsonNode) {
      items.add(parseItem(item));
    }
    return items;
  }

  /**
   * Convert a JSON document representing a single item returned by the works
   * API call into a Work object.
   *
   * @param jsonNode JSON document containing attributes of an item.
   * @return The Work object created from the JSON argument.
   * @throws IOException Thrown if an exception occurs during parsing.
   */
  private static Work parseItem(JsonNode jsonNode)
      throws IOException {
    Work work = new Work();
    work.setDOI(jsonNode.get("DOI").asText());
    // Note that sometimes one or more fields will be missing in the JSON
    // document, so we need to test for that before trying to populate the
    // associated object attribute.
    if (jsonNode.get("publisher") == null) {
      log.warn("Publisher is null for DOI=" + work.getDOI());
      work.setPublisher("");
    } else {
      work.setPublisher(jsonNode.get("publisher").asText());
    }
    // Titles are actually an array in the JSON document, so we need to convert
    // accordingly:
    if (jsonNode.get("title") == null) {
      log.warn("Title is null for DOI=" + work.getDOI());
      work.setTitles(new ArrayList<>());
    } else {
      work.setTitles(new ObjectMapper().readValue(
          jsonNode.get("title").toString(), new TypeReference<List<String>>() {
          }));
    }
    work.setTimestamp(jsonNode.get("created").get("timestamp").asLong());
    // Authors are represented as a JSON array of author objects, so we'll
    // iterate through each author and extract the name fields:
    List<String> l = new ArrayList<>();
    if (jsonNode.get("author") == null) {
      log.warn("Author is null for DOI=" + work.getDOI());
      work.setAuthors(new ArrayList<>());
    } else {
      for (JsonNode arrayNode : jsonNode.get("author")) {
        l.add(arrayNode.findPath("given").textValue() +
            " " +
            arrayNode.findPath("family").textValue());
      }
      work.setAuthors(l);
    }
    return work;
  }

  @Override
  public String toString() {
    return "Work{" +
        "DOI='" + DOI + '\'' +
        ", titles=" + titles +
        ", authors=" + authors +
        ", publisher='" + publisher + '\'' +
        ", timestamp=" + timestamp +
        '}';
  }
}
