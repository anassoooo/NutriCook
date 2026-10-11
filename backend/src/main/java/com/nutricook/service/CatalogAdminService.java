package com.nutricook.service;

import com.nutricook.domain.DomainException;
import com.nutricook.domain.ErrorCode;
import com.nutricook.domain.catalog.FoodQualityGate;
import com.nutricook.domain.model.FoodRole;
import com.nutricook.domain.model.Nutrients;
import com.nutricook.domain.model.PortionRule;
import com.nutricook.dto.request.FoodRequest;
import com.nutricook.entity.FoodItem;
import com.nutricook.entity.enums.FoodOrigin;
import com.nutricook.exception.ResourceNotFoundException;
import com.nutricook.repository.FoodItemRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Curated catalog administration (BR-CAT-01..05). Every saved food passes {@link FoodQualityGate};
 * foods are deactivated, never deleted, so plans that used them keep their history (BR-CAT-04,
 * BR-LIF-05). Seeded foods change through foods.csv, so here they can only be (de)activated.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CatalogAdminService {

  private final FoodItemRepository foods;

  /**
   * @param query part of the name, any case; {@code null} for all
   * @param role {@code null} for every role
   * @param active {@code null} for active and inactive foods
   */
  @Transactional(readOnly = true)
  public Page<FoodItem> search(String query, FoodRole role, Boolean active, int page, int size) {
    Specification<FoodItem> spec = Specification.where(null);
    if (query != null && !query.isBlank()) {
      String pattern = "%" + escapeLike(query.trim().toLowerCase(Locale.ROOT)) + "%";
      spec = spec.and((f, q, cb) -> cb.like(cb.lower(f.get("name")), pattern, '\\'));
    }
    if (role != null) spec = spec.and((f, q, cb) -> cb.equal(f.get("role"), role));
    if (active != null) spec = spec.and((f, q, cb) -> cb.equal(f.get("active"), active));
    return foods.findAll(spec, PageRequest.of(page, size, Sort.by("name")));
  }

  @Transactional(readOnly = true)
  public FoodItem get(Long id) {
    return foods
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Food not found: " + id));
  }

  /**
   * @throws DomainException VALIDATION_FAILED when the name is taken or the food fails the gate
   */
  @Transactional
  public FoodItem create(FoodRequest req) {
    if (foods.existsByNameIgnoreCase(req.name().trim())) throw nameTaken();
    FoodItem food = FoodItem.builder().origin(FoodOrigin.ADMIN).active(true).build();
    apply(food, req);
    assertPasses(food);
    FoodItem saved = save(food);
    log.info("Catalog: created food {} '{}'", saved.getId(), saved.getName());
    return saved;
  }

  /**
   * Plans already generated keep their snapshot of the old values (BR-LIF-05).
   *
   * @throws DomainException FOOD_READ_ONLY for a seeded food, VALIDATION_FAILED as for create
   */
  @Transactional
  public FoodItem update(Long id, FoodRequest req) {
    FoodItem food = get(id);
    if (food.getOrigin() == FoodOrigin.SEED) {
      throw new DomainException(
          ErrorCode.FOOD_READ_ONLY,
          "This food comes from the curated catalog file (foods.csv); change it there.",
          Map.of("origin", FoodOrigin.SEED.name()));
    }
    if (foods.existsByNameIgnoreCaseAndIdNot(req.name().trim(), id)) throw nameTaken();
    apply(food, req);
    assertPasses(food);
    FoodItem saved = save(food);
    log.info("Catalog: updated food {} '{}'", saved.getId(), saved.getName());
    return saved;
  }

  /** BR-CAT-04: no longer offered for new plans; existing plans are untouched. Idempotent. */
  @Transactional
  public FoodItem deactivate(Long id) {
    FoodItem food = get(id);
    food.setActive(false);
    log.info("Catalog: deactivated food {} '{}'", food.getId(), food.getName());
    return foods.save(food);
  }

  /**
   * Offers the food again. It must pass the gate as it is now, since older rows predate it.
   *
   * @throws DomainException VALIDATION_FAILED listing what fails
   */
  @Transactional
  public FoodItem activate(Long id) {
    FoodItem food = get(id);
    assertPasses(food);
    food.setActive(true);
    log.info("Catalog: activated food {} '{}'", food.getId(), food.getName());
    return foods.save(food);
  }

  /** The gate problems of a stored food, plus a missing source (BR-CAT-01). */
  public static List<String> problemsOf(FoodItem food) {
    List<String> problems = new ArrayList<>();
    if (food.getSource() == null || food.getSource().isBlank()) problems.add("no source");
    try {
      problems.addAll(FoodQualityGate.problems(FoodMapper.toView(food)));
    } catch (IllegalArgumentException e) {
      // invalid portion rule or negative nutrient values
      problems.add(e.getMessage());
    }
    return problems;
  }

  private static void assertPasses(FoodItem food) {
    List<Map<String, String>> errors = new ArrayList<>();
    Nutrients per100g = null;
    try {
      per100g = FoodMapper.per100g(food);
    } catch (IllegalArgumentException e) {
      errors.add(error("nutrients", e.getMessage()));
    }
    try {
      new PortionRule(
          food.getPortionMinG(),
          food.getPortionMaxG(),
          food.getPortionStepG(),
          food.getDefaultPortionG());
    } catch (IllegalArgumentException e) {
      errors.add(error("portion", e.getMessage()));
    }
    if (per100g != null) {
      FoodQualityGate.problems(per100g).forEach(p -> errors.add(error("nutrients", p)));
    }
    FoodQualityGate.labelProblems(food.getAllergens(), food.getDietFlags(), food.isMeat())
        .forEach(p -> errors.add(error("dietFlags", p)));
    if (food.getSuitableSlots().isEmpty()) errors.add(error("suitableSlots", "no meal slot"));
    if (food.getSource() == null || food.getSource().isBlank()) {
      errors.add(error("source", "is required"));
    }
    // reactivating a legacy row: its allergens may never have been verified (BR-SAF-03)
    if (!food.isAllergensKnown()) errors.add(error("allergens", "are not verified"));
    if (!errors.isEmpty()) {
      throw new DomainException(
          ErrorCode.VALIDATION_FAILED,
          "This food fails the catalog quality checks.",
          Map.of("errors", errors));
    }
  }

  private static void apply(FoodItem food, FoodRequest req) {
    food.setName(req.name().trim());
    food.setCategory(
        req.category() == null || req.category().isBlank() ? null : req.category().trim());
    food.setRole(req.role());
    food.setState(req.state());
    food.setCaloriesPer100g(req.caloriesPer100g());
    food.setProteinPer100g(req.proteinPer100g());
    food.setCarbsPer100g(req.carbsPer100g());
    food.setFatPer100g(req.fatPer100g());
    food.setSugarPer100g(req.sugarPer100g());
    food.setFiberPer100g(req.fiberPer100g());
    food.setSodiumMgPer100g(req.sodiumMgPer100g());
    food.setPortionMinG(req.portionMinG());
    food.setPortionMaxG(req.portionMaxG());
    food.setPortionStepG(req.portionStepG());
    food.setDefaultPortionG(req.defaultPortionG());
    food.setAllergens(new HashSet<>(req.allergens()));
    food.setDietFlags(new HashSet<>(req.dietFlags()));
    food.setSuitableSlots(new HashSet<>(req.suitableSlots()));
    food.setMeat(req.meat());
    food.setSource(req.source().trim());
    // the admin declared the allergen list, so it is verified (BR-SAF-03)
    food.setAllergensKnown(true);
  }

  /** The case-insensitive unique index still guards against a concurrent create. */
  private FoodItem save(FoodItem food) {
    try {
      return foods.saveAndFlush(food);
    } catch (DataIntegrityViolationException e) {
      String cause = String.valueOf(e.getMostSpecificCause().getMessage());
      if (cause.contains("ux_food_items_name_ci") || cause.contains("uq_food_items_name")) {
        throw nameTaken();
      }
      throw e;
    }
  }

  private static DomainException nameTaken() {
    return new DomainException(
        ErrorCode.VALIDATION_FAILED,
        "A food with this name already exists.",
        Map.of("errors", List.of(error("name", "already exists"))));
  }

  private static Map<String, String> error(String field, String message) {
    Map<String, String> m = new LinkedHashMap<>();
    m.put("field", field);
    m.put("message", message);
    return m;
  }

  private static String escapeLike(String s) {
    return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }
}
