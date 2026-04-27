package com.nutricook.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutricook.dto.response.LocationDTO;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class LocationService {

  private final RestTemplate restTemplate = new RestTemplate();
  private final ObjectMapper mapper = new ObjectMapper();

  @Value("${app.osm.overpass-url}")
  private String overpassUrl;

  @Value("${app.osm.user-agent}")
  private String userAgent;

  @Value("${app.osm.search-radius-meters:2000}")
  private int radiusMeters;

  @Cacheable(value = "locations", key = "#lat + ':' + #lng + ':restaurants:' + #restrictions")
  public List<LocationDTO> getNearbyRestaurants(double lat, double lng, List<String> restrictions) {
    String query = buildOverpassQuery(lat, lng, "restaurant");
    return fetchLocations(query, "RESTAURANT");
  }

  @Cacheable(value = "locations", key = "#lat + ':' + #lng + ':stores'")
  public List<LocationDTO> getNearbyStores(double lat, double lng) {
    String query = buildOverpassQuery(lat, lng, "supermarket");
    return fetchLocations(query, "GROCERY_STORE");
  }

  private List<LocationDTO> fetchLocations(String query, String type) {
    HttpHeaders headers = new HttpHeaders();
    headers.set("User-Agent", userAgent);
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    String encoded = "data=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
    HttpEntity<String> entity = new HttpEntity<>(encoded, headers);

    try {
      ResponseEntity<String> response =
          restTemplate.exchange(overpassUrl, HttpMethod.POST, entity, String.class);
      return parseOverpassResponse(response.getBody(), type);
    } catch (Exception e) {
      log.warn("Overpass API error: {}", e.getMessage());
      return List.of();
    }
  }

  private List<LocationDTO> parseOverpassResponse(String json, String type) {
    List<LocationDTO> results = new ArrayList<>();
    try {
      JsonNode root = mapper.readTree(json);
      for (JsonNode el : root.path("elements")) {
        JsonNode tags = el.path("tags");
        results.add(
            LocationDTO.builder()
                .name(tags.path("name").asText("Unknown"))
                .type(type)
                .latitude(el.path("lat").asDouble())
                .longitude(el.path("lon").asDouble())
                .address(buildAddress(tags))
                .osmNodeId(el.path("id").asLong())
                .build());
      }
    } catch (Exception e) {
      log.warn("Failed to parse Overpass response: {}", e.getMessage());
    }
    return results;
  }

  private String buildOverpassQuery(double lat, double lng, String amenity) {
    return String.format(
        "[out:json];node[\"amenity\"=\"%s\"](around:%d,%.6f,%.6f);out body;",
        amenity, radiusMeters, lat, lng);
  }

  private String buildAddress(JsonNode tags) {
    String street = tags.path("addr:street").asText("");
    String city = tags.path("addr:city").asText("");
    return (street + " " + city).trim();
  }
}
