package com.nutricook.domain.fixtures;

import com.nutricook.domain.model.Allergen;
import com.nutricook.domain.model.DietFlag;
import com.nutricook.domain.model.FoodRole;
import com.nutricook.domain.model.FoodState;
import com.nutricook.domain.model.FoodView;
import com.nutricook.domain.model.Nutrients;
import com.nutricook.domain.model.PortionRule;
import com.nutricook.domain.model.Slot;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * The real seeded catalog (db/catalog/foods.csv) as domain views, so composer tests run on the same
 * 84 foods production uses. Ids are the 1-based row numbers.
 */
public final class TestCatalog {

  private static final List<FoodView> FOODS = load();

  private TestCatalog() {}

  public static List<FoodView> all() {
    return FOODS;
  }

  public static FoodView named(String name) {
    return FOODS.stream()
        .filter(f -> f.name().equals(name))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("not in catalog: " + name));
  }

  private static List<FoodView> load() {
    try (var in = TestCatalog.class.getClassLoader().getResourceAsStream("db/catalog/foods.csv");
        var reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
      List<String> lines =
          reader.lines().filter(l -> !l.isBlank() && !l.startsWith("#")).skip(1).toList();
      List<FoodView> foods = new ArrayList<>();
      long id = 1;
      for (String line : lines) foods.add(parse(id++, line.split(",", -1)));
      return List.copyOf(foods);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static FoodView parse(long id, String[] c) {
    double[] n = new double[11];
    for (int i = 0; i < 11; i++) n[i] = Double.parseDouble(c[4 + i]);
    return new FoodView(
        id,
        c[0],
        c[1],
        FoodRole.valueOf(c[2]),
        FoodState.valueOf(c[3]),
        new Nutrients(n[0], n[1], n[2], n[3], n[4], n[5], n[6]),
        enums(Allergen.class, c[15]),
        true,
        enums(DietFlag.class, c[16]),
        Boolean.parseBoolean(c[17]),
        new PortionRule(n[7], n[8], n[9], n[10]),
        slots(c[18]));
  }

  private static <E extends Enum<E>> Set<E> enums(Class<E> type, String cell) {
    Set<E> set = EnumSet.noneOf(type);
    if (!cell.isEmpty()) for (String v : cell.split("\\|")) set.add(Enum.valueOf(type, v));
    return set;
  }

  private static Set<Slot> slots(String cell) {
    Set<Slot> set = EnumSet.noneOf(Slot.class);
    for (String code : cell.split("\\|")) {
      switch (code) {
        case "B" -> set.add(Slot.BREAKFAST);
        case "S" -> {
          set.add(Slot.MORNING_SNACK);
          set.add(Slot.AFTERNOON_SNACK);
        }
        case "L" -> set.add(Slot.LUNCH);
        case "D" -> set.add(Slot.DINNER);
        default -> throw new IllegalArgumentException("slot code " + code);
      }
    }
    return set;
  }
}
