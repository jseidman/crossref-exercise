package com.fivetran.crossref;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
public class CrossrefClient {
  private static String HOST = "api.crossref.org";
  private static String WORKS_RESOURCE = "works";
  private OkHttpClient client;

  public CrossrefClient() {
    client =
        new OkHttpClient.Builder()
            .readTimeout(20, TimeUnit.SECONDS)
            .build();
  }

  public WorksResponse works(WorksRequest worksRequest)
      throws IOException {
    HttpUrl url = getUrl(worksRequest);
    log.info("Works query URL=" + url);

    Request httpRequest = new Request.Builder().url(url).build();
    WorksResponse worksResponse;

    try (Response response = client.newCall(httpRequest).execute()) {
      if (!response.isSuccessful()) {
        throw new IOException(" " + response);
      }

      String json = response.body().string();
      ObjectMapper objectMapper = new ObjectMapper();
      JsonNode jsonNode = objectMapper.readTree(json);
      JsonNode items = jsonNode.get("message").get("items");
      worksResponse = WorksResponse.builder()
          .totalResults(jsonNode.get("message").get("total-results").asInt())
          .nextCursor(jsonNode.get("message").get("next-cursor").asText())
          .works(Work.parseItems(items))
          .build();
    }

    return worksResponse;
  }

  private HttpUrl getUrl(WorksRequest request) {
    HttpUrl.Builder builder = new HttpUrl.Builder()
        .scheme("https")
        .host(HOST)
        .addPathSegment(WORKS_RESOURCE);
    if (isNotBlank(request.getQuery())) {
      builder.addQueryParameter("query", request.getQuery());
    }
    if (isNotBlank(request.getFilter())) {
      builder.addQueryParameter("filter", request.getFilter());
    }
    if (isNotBlank(request.getSelectFields())) {
      builder.addQueryParameter("select", request.getSelectFields());
    }
    if (request.getRows() > 0) {
      builder.addQueryParameter("rows", "" + request.getRows());
    }
    if (isNotBlank(request.getCursor())) {
      builder.addQueryParameter("cursor", request.getCursor());
    }
    return builder.build();
  }
}
