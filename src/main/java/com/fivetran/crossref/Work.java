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

@Getter
@Setter
@Slf4j
public class Work {
  private String DOI;
  private List<String> titles;
  private List<String> authors;
  private String publisher;
  private Long timestamp;

  public static List<Work> parseItems(JsonNode jsonNode)
      throws IOException {
    List<Work> items = new ArrayList<>();
    for (JsonNode item : jsonNode) {
      items.add(parseItem(item));
    }
    return items;
  }

  public static Work parseItem(JsonNode jsonNode)
      throws IOException {
    Work work = new Work();
    work.setDOI(jsonNode.get("DOI").asText());
    if (jsonNode.get("publisher") == null) {
      log.warn("Publisher is null for DOI=" + work.getDOI());
      work.setPublisher("");
    } else {
      work.setPublisher(jsonNode.get("publisher").asText());
    }
    if (jsonNode.get("title") == null) {
      log.warn("Title is null for DOI=" + work.getDOI());
      work.setTitles(new ArrayList<>());
    } else {
      work.setTitles(new ObjectMapper().readValue(
          jsonNode.get("title").toString(), new TypeReference<List<String>>() {
          }));
    }
    work.setTimestamp(jsonNode.get("created").get("timestamp").asLong());
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
