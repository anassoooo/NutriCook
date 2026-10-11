package com.nutricook.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutricook.config.OsmConfig;
import com.nutricook.domain.DomainException;
import com.nutricook.domain.ErrorCode;
import com.nutricook.dto.response.LocationDTO;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Nearby restaurants and stores from OpenStreetMap's public Overpass API (no key needed).
 *
 * <ul>
 *   <li>Positions are rounded to a grid of about 100 m, for the query and the cache key alike, so
 *       nearby users share cached results and a user's exact position is never sent.
 *   <li>The servers in {@code app.osm.overpass-urls} are tried in order; only a JSON answer counts.
 *   <li>When every server fails, {@code MAP_UNAVAILABLE} is thrown. Exceptions are never cached, so
 *       the next request tries again, instead of serving "nothing nearby" for the cache lifetime.
 * </ul>
 */
@Service
@Slf4j
public class LocationService {

  /** Grid step in degrees: 0.001° is about 111 m of latitude. */
  static final double GRID = 0.001;

  /** Places returned per search (Overpass orders them by OSM id, not by distance). */
  static final int MAX_RESULTS = 50;

  private final RestTemplate http;
  private final List<String> overpassUrls;
  private final String userAgent;
  private final int radiusMeters;
  private final ObjectMapper mapper = new ObjectMapper();

  public LocationService(
      @Qualifier("overpassRestTemplate") RestTemplate http,
      @Value("${app.osm.overpass-urls}") String overpassUrls,
      @Value("${app.osm.user-agent}") String userAgent,
      @Value("${app.osm.search-radius-meters:2000}") int radiusMeters) {
    this.http = http;
    // split here rather than relying on a conversion service, which not every context has
    this.overpassUrls =
        Arrays.stream(overpassUrls.split(",")).map(String::trim).filter(u -> !u.isEmpty()).toList();
    if (this.overpassUrls.isEmpty()) {
      throw new IllegalStateException("app.osm.overpass-urls must list at least one URL");
    }
    this.userAgent = userAgent;
    this.radiusMeters = radiusMeters;
  }

  /**
   * @throws DomainException MAP_UNAVAILABLE when no Overpass server answers
   */
  @Cacheable(
      value = "locations",
      key = "T(com.nutricook.service.LocationService).cell(#lat, #lng) + ':restaurants'")
  public List<LocationDTO> getNearbyRestaurants(double lat, double lng) {
    return fetch(query(lat, lng, "amenity", "restaurant"), "RESTAURANT");
  }

  /**
   * @throws DomainException MAP_UNAVAILABLE when no Overpass server answers
   */
  @Cacheable(
      value = "locations",
      key = "T(com.nutricook.service.LocationService).cell(#lat, #lng) + ':stores'")
  public List<LocationDTO> getNearbyStores(double lat, double lng) {
    // OpenStreetMap tags supermarkets as shops, not amenities
    return fetch(query(lat, lng, "shop", "supermarket"), "GROCERY_STORE");
  }

  /** The grid cell of a position, e.g. {@code 36.807:10.182}; also the cache key. */
  public static String cell(double lat, double lng) {
    return String.format(Locale.ROOT, "%.3f:%.3f", snap(lat), snap(lng));
  }

  static double snap(double degrees) {
    return Math.round(degrees / GRID) * GRID;
  }

  /**
   * Overpass QL with a server-side timeout; numbers formatted independently of the JVM locale.
   * {@code nwr} includes places mapped as buildings (ways), which {@code out center} reduces to a
   * point.
   */
  String query(double lat, double lng, String key, String value) {
    return String.format(
        Locale.ROOT,
        "[out:json][timeout:%d];nwr[\"%s\"=\"%s\"](around:%d,%.3f,%.3f);out center %d;",
        OsmConfig.QUERY_TIMEOUT_SECONDS,
        key,
        value,
        radiusMeters,
        snap(lat),
        snap(lng),
        MAX_RESULTS);
  }

  private List<LocationDTO> fetch(String query, String type) {
    HttpHeaders headers = new HttpHeaders();
    headers.set(HttpHeaders.USER_AGENT, userAgent);
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    HttpEntity<String> request =
        new HttpEntity<>("data=" + URLEncoder.encode(query, StandardCharsets.UTF_8), headers);

    for (String url : overpassUrls) {
      long start = System.nanoTime();
      try {
        ResponseEntity<String> response =
            http.exchange(url, HttpMethod.POST, request, String.class);
        List<LocationDTO> places = parse(response.getBody(), type);
        log.info(
            "overpass_call url={} outcome=ok results={} ms={}",
            url,
            places.size(),
            (System.nanoTime() - start) / 1_000_000);
        return places;
      } catch (Exception e) {
        // overloaded servers answer with an HTML error page, sometimes with status 200
        log.warn(
            "overpass_call url={} outcome=error error={} ms={}",
            url,
            e.getClass().getSimpleName(),
            (System.nanoTime() - start) / 1_000_000);
      }
    }
    throw new DomainException(
        ErrorCode.MAP_UNAVAILABLE,
        "Nearby places are unavailable right now. Please try again in a minute.");
  }

  /**
   * @throws IllegalStateException when the body is not an Overpass JSON answer
   */
  private List<LocationDTO> parse(String body, String type) throws Exception {
    JsonNode root = body == null ? null : mapper.readTree(body);
    if (root == null || !root.has("elements")) {
      throw new IllegalStateException("not an Overpass JSON answer");
    }
    // a busy server may answer JSON whose remark reports a runtime error, with partial or no data
    if (root.path("remark").asText("").contains("error")) {
      throw new IllegalStateException("Overpass reported an error");
    }
    List<LocationDTO> results = new ArrayList<>();
    for (JsonNode el : root.path("elements")) {
      JsonNode tags = el.path("tags");
      // nodes carry lat/lon; ways and relations carry a center
      JsonNode point = el.has("lat") ? el : el.path("center");
      if (!point.has("lat")) continue;
      results.add(
          LocationDTO.builder()
              .name(tags.path("name").asText("Unknown"))
              .type(type)
              .latitude(point.path("lat").asDouble())
              .longitude(point.path("lon").asDouble())
              .address(address(tags))
              .osmNodeId(el.path("id").asLong())
              .build());
    }
    return results;
  }

  private static String address(JsonNode tags) {
    String street = tags.path("addr:street").asText("");
    String city = tags.path("addr:city").asText("");
    return (street + " " + city).trim();
  }
}
