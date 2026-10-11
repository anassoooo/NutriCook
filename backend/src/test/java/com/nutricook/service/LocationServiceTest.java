package com.nutricook.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.nutricook.config.CacheConfig;
import com.nutricook.domain.DomainException;
import com.nutricook.domain.ErrorCode;
import com.nutricook.dto.response.LocationDTO;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

/**
 * Nearby places through the real cache proxy, against a scripted Overpass: failover, error pages,
 * failures never cached, shared grid cells.
 */
@SpringJUnitConfig(LocationServiceTest.Config.class)
@TestPropertySource(
    properties = {
      "app.osm.overpass-urls=https://main.test/api/interpreter,https://mirror.test/api/interpreter",
      "app.osm.user-agent=NutriCook-Test",
      "app.osm.search-radius-meters=2000",
      "app.cache.location-ttl-minutes=60"
    })
class LocationServiceTest {

  private static final String MAIN = "https://main.test/api/interpreter";
  private static final String MIRROR = "https://mirror.test/api/interpreter";

  private static final String TWO_PLACES =
      """
      {"elements":[
        {"id":1,"lat":36.8071,"lon":10.1822,"tags":{"name":"Le Capitole","addr:city":"Tunis"}},
        {"id":2,"type":"way","center":{"lat":36.8069,"lon":10.1819},"tags":{"name":"Steak Haus"}}]}
      """;

  /** What an overloaded Overpass server actually sends, with status 200. */
  private static final String BUSY_PAGE =
      "<html><body><p><strong>Error</strong>: runtime error: open64: 0 Success"
          + " /osm3s_osm_base Dispatcher_Client::request_read_and_idx</p></body></html>";

  @Configuration
  @EnableCaching
  @Import({CacheConfig.class, LocationService.class})
  static class Config {
    @Bean
    RestTemplate overpassRestTemplate() {
      return new RestTemplate();
    }
  }

  @Autowired LocationService locations;
  @Autowired RestTemplate overpassRestTemplate;
  @Autowired CacheManager caches;

  private MockRestServiceServer overpass;

  @BeforeEach
  void setUp() {
    caches.getCache("locations").clear();
    overpass = MockRestServiceServer.bindTo(overpassRestTemplate).ignoreExpectOrder(false).build();
  }

  @Test
  void answersFromTheMainServerWithAnIdentifyingUserAgent() {
    overpass
        .expect(once(), requestTo(MAIN))
        .andExpect(header("User-Agent", "NutriCook-Test"))
        .andRespond(withSuccess(TWO_PLACES, MediaType.APPLICATION_JSON));

    List<LocationDTO> places = locations.getNearbyRestaurants(36.8065, 10.1815);

    assertThat(places)
        .extracting(LocationDTO::getName)
        .containsExactly("Le Capitole", "Steak Haus");
    assertThat(places.get(0).getAddress()).isEqualTo("Tunis");
    // a restaurant mapped as a building is placed at its center
    assertThat(places.get(1).getLatitude()).isEqualTo(36.8069);
    overpass.verify();
  }

  @Test
  void fallsBackToTheMirrorWhenTheMainServerIsBusy() {
    overpass
        .expect(once(), requestTo(MAIN))
        .andRespond(withSuccess(BUSY_PAGE, MediaType.TEXT_HTML)); // status 200, not JSON
    overpass
        .expect(once(), requestTo(MIRROR))
        .andRespond(withSuccess(TWO_PLACES, MediaType.APPLICATION_JSON));

    assertThat(locations.getNearbyRestaurants(36.8065, 10.1815)).hasSize(2);
    overpass.verify();
  }

  @Test
  void aRuntimeErrorRemarkCountsAsAFailure() {
    overpass
        .expect(once(), requestTo(MAIN))
        .andRespond(
            withSuccess(
                "{\"elements\":[],\"remark\":\"runtime error: Query timed out\"}",
                MediaType.APPLICATION_JSON));
    overpass
        .expect(once(), requestTo(MIRROR))
        .andRespond(withSuccess(TWO_PLACES, MediaType.APPLICATION_JSON));

    assertThat(locations.getNearbyRestaurants(36.8065, 10.1815)).hasSize(2);
    overpass.verify();
  }

  @Test
  void whenEveryServerFailsTheUserIsToldAndNothingIsCached() {
    overpass.expect(once(), requestTo(MAIN)).andRespond(withServerError());
    overpass
        .expect(once(), requestTo(MIRROR))
        .andRespond(withSuccess(BUSY_PAGE, MediaType.TEXT_HTML));

    assertThatThrownBy(() -> locations.getNearbyRestaurants(36.8065, 10.1815))
        .isInstanceOfSatisfying(
            DomainException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.MAP_UNAVAILABLE));
    overpass.verify();

    // the next request asks again instead of serving an empty list for an hour
    overpass.reset();
    overpass
        .expect(once(), requestTo(MAIN))
        .andRespond(withSuccess(TWO_PLACES, MediaType.APPLICATION_JSON));
    assertThat(locations.getNearbyRestaurants(36.8065, 10.1815)).hasSize(2);
    overpass.verify();
  }

  @Test
  void nearbyUsersShareOneCachedAnswerPerGridCell() {
    overpass
        .expect(once(), requestTo(MAIN))
        .andRespond(withSuccess(TWO_PLACES, MediaType.APPLICATION_JSON));

    locations.getNearbyRestaurants(36.80652, 10.18148);
    // about 20 m away: same cell, served from the cache
    List<LocationDTO> second = locations.getNearbyRestaurants(36.80668, 10.18132);

    assertThat(second).hasSize(2);
    assertThat(LocationService.cell(36.80652, 10.18148))
        .isEqualTo(LocationService.cell(36.80668, 10.18132))
        .isEqualTo("36.807:10.181");
    overpass.verify(); // exactly one call
  }

  @Test
  void restaurantsAndStoresAreCachedSeparately() {
    overpass
        .expect(once(), requestTo(MAIN))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("restaurant")))
        .andRespond(withSuccess(TWO_PLACES, MediaType.APPLICATION_JSON));
    overpass
        .expect(once(), requestTo(MAIN))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("shop")))
        .andRespond(withSuccess("{\"elements\":[]}", MediaType.APPLICATION_JSON));

    assertThat(locations.getNearbyRestaurants(36.8065, 10.1815)).hasSize(2);
    assertThat(locations.getNearbyStores(36.8065, 10.1815)).isEmpty(); // a real "none nearby"
    overpass.verify();
  }

  @Test
  void theQueryIsTheSameWhateverTheJvmLocale() {
    Locale previous = Locale.getDefault();
    try {
      Locale.setDefault(Locale.FRANCE); // decimal comma
      String query = locations.query(36.8065, 10.1815, "amenity", "restaurant");
      assertThat(query)
          .isEqualTo(
              "[out:json][timeout:15];nwr[\"amenity\"=\"restaurant\"]"
                  + "(around:2000,36.807,10.182);out center 50;");
      assertThat(LocationService.cell(36.8065, 10.1815)).isEqualTo("36.807:10.182");
    } finally {
      Locale.setDefault(previous);
    }
  }
}
