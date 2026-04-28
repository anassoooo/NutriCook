package com.nutricook.init;

import com.nutricook.entity.*;
import com.nutricook.entity.enums.AchievementType;
import com.nutricook.entity.enums.RestrictionType;
import com.nutricook.entity.enums.Role;
import com.nutricook.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

  private final HealthConditionRepository conditionRepo;
  private final DietaryRestrictionRepository restrictionRepo;
  private final AchievementRepository achievementRepo;
  private final FoodItemRepository foodRepo;
  private final UserRepository userRepo;
  private final UserProfileRepository profileRepo;
  private final PasswordEncoder passwordEncoder;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    seedAdminUser();
    seedHealthConditions();
    seedDietaryRestrictions();
    seedAchievements();
    seedFoodItems();
    log.info("Data initialization complete.");
  }

  private void seedAdminUser() {
    if (userRepo.existsByEmail("admin@nutricook.dev")) return;
    User admin =
        User.builder()
            .email("admin@nutricook.dev")
            .passwordHash(passwordEncoder.encode("Admin1234!"))
            .role(Role.ADMIN)
            .build();
    userRepo.save(admin);
    profileRepo.save(UserProfile.builder().user(admin).build());
    log.info("Admin user created: admin@nutricook.dev / Admin1234!");
  }

  private void seedHealthConditions() {
    if (conditionRepo.count() > 0) return;
    conditionRepo.save(
        HealthCondition.builder()
            .name("Diabetes")
            .description("Type 1 or Type 2 diabetes")
            .sugarLimitG(15.0)
            .build());
    conditionRepo.save(
        HealthCondition.builder()
            .name("High Cholesterol")
            .description("Elevated LDL levels")
            .fatLimitG(25.0)
            .build());
    conditionRepo.save(
        HealthCondition.builder().name("Hypertension").description("High blood pressure").build());
    conditionRepo.save(
        HealthCondition.builder().name("Celiac Disease").description("Gluten intolerance").build());
    conditionRepo.save(
        HealthCondition.builder()
            .name("Lactose Intolerance")
            .description("Dairy sugar intolerance")
            .build());
  }

  private void seedDietaryRestrictions() {
    if (restrictionRepo.count() > 0) return;
    restrictionRepo.save(
        DietaryRestriction.builder()
            .name("Vegan")
            .type(RestrictionType.PREFERENCE)
            .description("No animal products")
            .build());
    restrictionRepo.save(
        DietaryRestriction.builder()
            .name("Vegetarian")
            .type(RestrictionType.PREFERENCE)
            .description("No meat or fish")
            .build());
    restrictionRepo.save(
        DietaryRestriction.builder()
            .name("Gluten-Free")
            .type(RestrictionType.INTOLERANCE)
            .description("No gluten-containing foods")
            .build());
    restrictionRepo.save(
        DietaryRestriction.builder()
            .name("Lactose-Free")
            .type(RestrictionType.INTOLERANCE)
            .description("No dairy products")
            .build());
    restrictionRepo.save(
        DietaryRestriction.builder()
            .name("Nut Allergy")
            .type(RestrictionType.ALLERGY)
            .description("No tree nuts or peanuts")
            .build());
    restrictionRepo.save(
        DietaryRestriction.builder()
            .name("Halal")
            .type(RestrictionType.RELIGIOUS)
            .description("Halal-certified foods only")
            .build());
    restrictionRepo.save(
        DietaryRestriction.builder()
            .name("Kosher")
            .type(RestrictionType.RELIGIOUS)
            .description("Kosher-certified foods only")
            .build());
  }

  private void seedAchievements() {
    if (achievementRepo.count() > 0) return;
    achievementRepo.save(
        Achievement.builder()
            .name("First Steps")
            .description("Generated your first diet plan")
            .iconName("seedling")
            .type(AchievementType.PLANS_GENERATED)
            .threshold(1)
            .build());
    achievementRepo.save(
        Achievement.builder()
            .name("Planner")
            .description("Generated 5 diet plans")
            .iconName("clipboard")
            .type(AchievementType.PLANS_GENERATED)
            .threshold(5)
            .build());
    achievementRepo.save(
        Achievement.builder()
            .name("Week Warrior")
            .description("Logged progress 7 days in a row")
            .iconName("fire")
            .type(AchievementType.LOGIN_STREAK)
            .threshold(7)
            .build());
    achievementRepo.save(
        Achievement.builder()
            .name("Consistent")
            .description("Logged progress 30 days in a row")
            .iconName("star")
            .type(AchievementType.LOGIN_STREAK)
            .threshold(30)
            .build());
    achievementRepo.save(
        Achievement.builder()
            .name("Hydration Hero")
            .description("Drank 2L water for 7 days")
            .iconName("droplet")
            .type(AchievementType.HYDRATION)
            .threshold(7)
            .build());
    achievementRepo.save(
        Achievement.builder()
            .name("Calorie Champion")
            .description("Hit calorie goal 10 days")
            .iconName("trophy")
            .type(AchievementType.CALORIE_GOAL)
            .threshold(10)
            .build());
  }

  private void seedFoodItems() {
    if (foodRepo.count() > 0) return;

    // ── Fruits ────────────────────────────────────────────────────────
    foodRepo.save(fi("Apple", "fruit", 52, 0.3, 14.0, 0.2, 10.4, 2.4, 1.0));
    foodRepo.save(fi("Banana", "fruit", 89, 1.1, 23.0, 0.3, 12.2, 2.6, 1.0));
    foodRepo.save(fi("Avocado", "fruit", 160, 2.0, 9.0, 15.0, 0.7, 6.7, 7.0));
    foodRepo.save(fi("Orange", "fruit", 47, 0.9, 11.8, 0.1, 9.4, 2.4, 1.0));
    foodRepo.save(fi("Strawberries", "fruit", 32, 0.7, 7.7, 0.3, 4.9, 2.0, 1.0));
    foodRepo.save(fi("Blueberries", "fruit", 57, 0.7, 14.5, 0.3, 10.0, 2.4, 1.0));
    foodRepo.save(fi("Mango", "fruit", 60, 0.8, 15.0, 0.4, 13.7, 1.6, 1.0));
    foodRepo.save(fi("Watermelon", "fruit", 30, 0.6, 7.6, 0.2, 6.2, 0.4, 1.0));
    foodRepo.save(fi("Pineapple", "fruit", 50, 0.5, 13.1, 0.1, 9.9, 1.4, 1.0));
    foodRepo.save(fi("Pear", "fruit", 57, 0.4, 15.2, 0.1, 9.8, 3.1, 1.0));

    // ── Vegetables ───────────────────────────────────────────────────
    foodRepo.save(fi("Spinach", "vegetable", 23, 2.9, 3.6, 0.4, 0.4, 2.2, 79.0));
    foodRepo.save(fi("Sweet Potato", "vegetable", 86, 1.6, 20.0, 0.1, 4.2, 3.0, 55.0));
    foodRepo.save(fi("Broccoli", "vegetable", 34, 2.8, 6.6, 0.4, 1.7, 2.6, 33.0));
    foodRepo.save(fi("Carrot", "vegetable", 41, 0.9, 9.6, 0.2, 4.7, 2.8, 69.0));
    foodRepo.save(fi("Tomato", "vegetable", 18, 0.9, 3.9, 0.2, 2.6, 1.2, 5.0));
    foodRepo.save(fi("Cucumber", "vegetable", 16, 0.7, 3.6, 0.1, 1.7, 0.5, 2.0));
    foodRepo.save(fi("Bell Pepper", "vegetable", 31, 1.0, 6.0, 0.3, 4.2, 2.1, 4.0));
    foodRepo.save(fi("Kale", "vegetable", 49, 4.3, 8.8, 0.9, 2.3, 3.6, 38.0));
    foodRepo.save(fi("Cauliflower", "vegetable", 25, 1.9, 5.0, 0.3, 1.9, 2.0, 30.0));
    foodRepo.save(fi("Mushrooms", "vegetable", 22, 3.1, 3.3, 0.3, 2.0, 1.0, 5.0));
    foodRepo.save(fi("Zucchini", "vegetable", 17, 1.2, 3.1, 0.3, 2.5, 1.0, 8.0));
    foodRepo.save(fi("Green Beans", "vegetable", 31, 1.8, 7.1, 0.1, 3.3, 3.4, 6.0));

    // ── Grains (excluded for Gluten-Free) ────────────────────────────
    foodRepo.save(fi("Oatmeal", "grain", 389, 16.9, 66.0, 6.9, 0.0, 10.6, 2.0));
    foodRepo.save(fi("Brown Rice", "grain", 216, 4.5, 45.0, 1.8, 0.7, 3.5, 7.0));
    foodRepo.save(fi("Quinoa", "grain", 368, 14.1, 64.0, 6.1, 0.0, 7.0, 5.0));
    foodRepo.save(fi("White Rice", "grain", 130, 2.7, 28.6, 0.3, 0.1, 0.4, 1.0));
    foodRepo.save(fi("Whole Wheat Bread", "grain", 247, 13.0, 41.0, 3.4, 5.0, 7.0, 472.0));
    foodRepo.save(fi("White Pasta", "grain", 371, 13.0, 74.0, 1.5, 0.6, 2.5, 6.0));
    foodRepo.save(fi("Whole Wheat Pasta", "grain", 352, 13.5, 66.0, 2.5, 3.0, 8.7, 7.0));
    foodRepo.save(fi("Couscous", "grain", 376, 12.8, 72.0, 0.6, 0.0, 2.0, 10.0));
    foodRepo.save(fi("Corn Tortilla", "grain", 218, 5.7, 45.0, 2.5, 0.0, 6.3, 40.0));
    foodRepo.save(fi("Seitan", "grain", 370, 75.0, 14.0, 1.9, 0.0, 0.5, 400.0));

    // ── Legumes ──────────────────────────────────────────────────────
    foodRepo.save(fi("Lentils", "legume", 116, 9.0, 20.0, 0.4, 1.8, 7.9, 2.0));
    foodRepo.save(fi("Black Beans", "legume", 341, 21.6, 62.0, 1.4, 2.1, 15.5, 5.0));
    foodRepo.save(fi("Tofu", "legume", 76, 8.1, 1.9, 4.8, 0.0, 0.3, 7.0));
    foodRepo.save(fi("Chickpeas", "legume", 364, 19.3, 60.0, 6.0, 10.7, 17.4, 24.0));
    foodRepo.save(fi("Kidney Beans", "legume", 333, 23.6, 60.0, 0.8, 2.2, 15.3, 14.0));
    foodRepo.save(fi("Edamame", "legume", 121, 11.9, 8.9, 5.2, 3.4, 5.2, 63.0));
    foodRepo.save(fi("Green Peas", "legume", 81, 5.4, 14.5, 0.4, 5.7, 5.1, 5.0));
    foodRepo.save(fi("Tempeh", "legume", 192, 20.3, 7.6, 10.8, 0.0, 5.7, 9.0));
    foodRepo.save(fi("Peanut Butter", "legume", 588, 25.1, 20.0, 50.0, 9.0, 6.0, 340.0));

    // ── Nuts & Seeds (excluded for Nut Allergy) ───────────────────────
    foodRepo.save(fi("Almonds", "nut", 579, 21.2, 22.0, 49.9, 4.4, 12.5, 1.0));
    foodRepo.save(fi("Walnuts", "nut", 654, 15.2, 13.7, 65.2, 2.6, 6.7, 2.0));
    foodRepo.save(fi("Cashews", "nut", 553, 18.2, 30.2, 43.8, 5.9, 3.3, 12.0));
    foodRepo.save(fi("Chia Seeds", "nut", 486, 16.5, 42.1, 30.7, 0.0, 34.4, 16.0));
    foodRepo.save(fi("Flaxseeds", "nut", 534, 18.3, 28.9, 42.2, 1.6, 27.3, 30.0));
    foodRepo.save(fi("Sunflower Seeds", "nut", 584, 20.8, 20.0, 51.5, 2.6, 8.6, 9.0));
    foodRepo.save(fi("Mixed Nuts", "nut", 607, 14.9, 21.0, 54.1, 4.0, 7.4, 3.0));

    // ── Dairy (excluded for Vegan + Lactose-Free) ─────────────────────
    foodRepo.save(fi("Greek Yogurt", "dairy", 59, 10.2, 3.6, 0.4, 3.2, 0.0, 46.0));
    foodRepo.save(fi("Cottage Cheese", "dairy", 98, 11.1, 3.4, 4.3, 2.7, 0.0, 364.0));
    foodRepo.save(fi("Whole Milk", "dairy", 61, 3.2, 4.8, 3.3, 5.1, 0.0, 44.0));
    foodRepo.save(fi("Cheddar Cheese", "dairy", 402, 25.0, 1.3, 33.1, 0.5, 0.0, 621.0));
    foodRepo.save(fi("Mozzarella", "dairy", 280, 28.0, 3.1, 17.0, 1.0, 0.0, 627.0));
    foodRepo.save(fi("Whey Protein Powder", "dairy", 370, 80.0, 8.0, 4.0, 5.0, 0.0, 150.0));
    foodRepo.save(fi("Butter", "dairy", 717, 0.9, 0.1, 81.0, 0.1, 0.0, 643.0));

    // ── Eggs (excluded for Vegan only) ───────────────────────────────
    foodRepo.save(fi("Eggs", "egg", 155, 13.0, 1.1, 11.0, 0.0, 0.0, 124.0));
    foodRepo.save(fi("Egg Whites", "egg", 52, 10.9, 0.7, 0.2, 0.5, 0.0, 166.0));

    // ── Meat (excluded for Vegan + Vegetarian) ────────────────────────
    foodRepo.save(fi("Chicken Breast", "meat", 165, 31.0, 0.0, 3.6, 0.0, 0.0, 74.0));
    foodRepo.save(fi("Chicken Thigh", "meat", 209, 26.0, 0.0, 11.0, 0.0, 0.0, 84.0));
    foodRepo.save(fi("Turkey Breast", "meat", 135, 30.1, 0.0, 1.0, 0.0, 0.0, 70.0));
    foodRepo.save(fi("Beef (lean mince)", "meat", 215, 26.1, 0.0, 12.0, 0.0, 0.0, 84.0));

    // ── Seafood (excluded for Vegan + Vegetarian) ─────────────────────
    foodRepo.save(fi("Salmon", "seafood", 208, 20.0, 0.0, 13.0, 0.0, 0.0, 59.0));
    foodRepo.save(fi("Tuna (canned)", "seafood", 116, 25.5, 0.0, 1.0, 0.0, 0.0, 337.0));
    foodRepo.save(fi("Shrimp", "seafood", 99, 24.0, 0.2, 0.3, 0.0, 0.0, 111.0));
    foodRepo.save(fi("Cod", "seafood", 82, 17.8, 0.0, 0.7, 0.0, 0.0, 78.0));
    foodRepo.save(fi("Sardines (canned)", "seafood", 208, 24.6, 0.0, 11.5, 0.0, 0.0, 307.0));
    foodRepo.save(fi("Tilapia", "seafood", 96, 20.1, 0.0, 1.7, 0.0, 0.0, 56.0));
    foodRepo.save(fi("Mackerel", "seafood", 205, 18.6, 0.0, 13.9, 0.0, 0.0, 90.0));

    // ── Fats & Oils ──────────────────────────────────────────────────
    foodRepo.save(fi("Olive Oil", "fat", 884, 0.0, 0.0, 100.0, 0.0, 0.0, 2.0));
    foodRepo.save(fi("Coconut Oil", "fat", 862, 0.0, 0.0, 100.0, 0.0, 0.0, 0.0));

    // ════════════════════════════════════════════════════════════════
    // ── Tunisian Kitchen ─────────────────────────────────────────────
    // ════════════════════════════════════════════════════════════════

    // Fruits
    foodRepo.save(fi("Dates (Deglet Nour)", "fruit", 282, 2.5, 75.0, 0.4, 63.4, 8.0, 2.0));
    foodRepo.save(fi("Figs (fresh)", "fruit", 74, 0.8, 19.2, 0.3, 16.3, 2.9, 1.0));
    foodRepo.save(fi("Pomegranate", "fruit", 83, 1.7, 18.7, 1.2, 13.7, 4.0, 3.0));
    foodRepo.save(fi("Prickly Pear", "fruit", 41, 0.7, 9.6, 0.5, 5.4, 3.6, 5.0));

    // Vegetables
    foodRepo.save(fi("Eggplant", "vegetable", 25, 1.0, 5.9, 0.2, 3.5, 3.0, 2.0));
    foodRepo.save(fi("Artichoke", "vegetable", 47, 3.3, 10.5, 0.2, 0.9, 5.4, 94.0));
    foodRepo.save(fi("Fennel", "vegetable", 31, 1.2, 7.3, 0.2, 0.0, 3.1, 52.0));
    foodRepo.save(fi("Turnip", "vegetable", 28, 0.9, 6.4, 0.1, 3.8, 1.8, 67.0));
    foodRepo.save(fi("Pumpkin", "vegetable", 26, 1.0, 6.5, 0.1, 2.8, 0.5, 1.0));
    foodRepo.save(fi("Swiss Chard", "vegetable", 19, 1.8, 3.7, 0.2, 1.1, 1.6, 213.0));

    // Legumes
    foodRepo.save(fi("Fava Beans (cooked)", "legume", 110, 7.6, 19.6, 0.4, 1.7, 7.5, 7.0));
    foodRepo.save(fi("Split Peas (cooked)", "legume", 118, 8.3, 21.1, 0.4, 2.9, 8.1, 3.0));

    // Grains
    foodRepo.save(fi("Semolina", "grain", 360, 12.7, 72.8, 1.1, 0.0, 3.9, 1.0));
    foodRepo.save(fi("Bsissa", "grain", 380, 14.0, 68.0, 6.0, 0.0, 8.0, 5.0));

    // Nuts & Seeds
    foodRepo.save(fi("Pine Nuts", "nut", 673, 13.7, 13.1, 68.4, 3.6, 3.7, 2.0));
    foodRepo.save(fi("Pistachios", "nut", 562, 20.2, 27.2, 45.3, 7.7, 10.3, 1.0));
    foodRepo.save(fi("Sesame Seeds", "nut", 573, 17.7, 23.5, 49.7, 0.5, 11.8, 11.0));

    // Dairy
    foodRepo.save(fi("Leben (fermented milk)", "dairy", 56, 3.1, 4.4, 3.0, 4.4, 0.0, 45.0));

    // Meat
    foodRepo.save(fi("Lamb (lean)", "meat", 258, 25.4, 0.0, 17.0, 0.0, 0.0, 81.0));
    foodRepo.save(fi("Merguez", "meat", 350, 18.0, 2.0, 30.0, 0.0, 0.0, 900.0));
    foodRepo.save(fi("Kefta", "meat", 230, 20.0, 3.0, 15.0, 0.0, 0.0, 400.0));

    // Seafood
    foodRepo.save(fi("Sea Bass (Loup de mer)", "seafood", 97, 18.4, 0.0, 2.5, 0.0, 0.0, 63.0));
    foodRepo.save(fi("Sea Bream (Dorade)", "seafood", 96, 18.5, 0.0, 2.4, 0.0, 0.0, 65.0));
    foodRepo.save(fi("Red Mullet (Rouget)", "seafood", 109, 18.0, 0.0, 4.0, 0.0, 0.0, 72.0));
    foodRepo.save(fi("Grouper (Merou)", "seafood", 92, 19.4, 0.0, 1.2, 0.0, 0.0, 53.0));
    foodRepo.save(fi("Octopus", "seafood", 82, 14.9, 2.2, 1.0, 0.0, 0.0, 230.0));
    foodRepo.save(fi("Calamari (Squid)", "seafood", 92, 15.6, 3.1, 1.4, 0.0, 0.0, 44.0));

    // ── More Tunisian Kitchen ─────────────────────────────────────────

    // Additional seafood — Mediterranean species found in Tunisian markets
    foodRepo.save(fi("Swordfish (Espadon)", "seafood", 121, 19.8, 0.0, 4.0, 0.0, 0.0, 90.0));
    foodRepo.save(fi("Cuttlefish (Seiche)", "seafood", 79, 16.2, 0.8, 0.7, 0.0, 0.0, 300.0));
    foodRepo.save(fi("Grey Mullet (Mulet)", "seafood", 118, 20.0, 0.0, 3.8, 0.0, 0.0, 65.0));
    foodRepo.save(fi("Anchovy (Anchois)", "seafood", 131, 20.4, 0.0, 5.0, 0.0, 0.0, 104.0));

    // Additional meat — traditional Tunisian proteins
    foodRepo.save(fi("Goat Meat (lean)", "meat", 143, 27.1, 0.0, 3.0, 0.0, 0.0, 82.0));
    foodRepo.save(fi("Camel Meat (lean)", "meat", 103, 21.0, 0.0, 1.5, 0.0, 0.0, 68.0));

    // Additional legumes — staples of Tunisian cuisine
    foodRepo.save(fi("White Beans (Loubia)", "legume", 139, 9.7, 25.0, 0.5, 0.4, 6.3, 2.0));
    foodRepo.save(fi("Dried Broad Beans (Foul)", "legume", 341, 26.1, 58.3, 1.5, 0.0, 25.0, 13.0));

    // Additional vegetables — used in Tunisian dishes
    foodRepo.save(fi("Purslane (Blouza)", "vegetable", 20, 2.0, 3.4, 0.4, 0.0, 0.5, 45.0));
    foodRepo.save(fi("Cardoon (Cardon)", "vegetable", 17, 0.7, 3.8, 0.1, 0.0, 1.8, 80.0));
    foodRepo.save(fi("Leek", "vegetable", 61, 1.5, 14.2, 0.3, 3.9, 1.8, 20.0));

    // Additional grains
    foodRepo.save(fi("Barley (Orge)", "grain", 354, 12.5, 73.5, 2.3, 0.0, 17.3, 12.0));
    foodRepo.save(fi("Millet (Doukhn)", "grain", 378, 11.0, 72.8, 4.2, 0.0, 8.5, 5.0));

    // Dairy
    foodRepo.save(fi("Jben (fresh white cheese)", "dairy", 174, 11.3, 2.9, 13.0, 0.5, 0.0, 430.0));

    // Condiment — fundamental to Tunisian cooking
    foodRepo.save(fi("Harissa", "fat", 50, 2.0, 10.0, 1.0, 5.0, 2.0, 500.0));
    foodRepo.save(fi("Capers (Câpres)", "vegetable", 23, 2.4, 4.9, 0.9, 1.0, 3.2, 2960.0));
  }

  private FoodItem fi(
      String name,
      String cat,
      double cal,
      double prot,
      double carb,
      double fat,
      double sugar,
      double fiber,
      double sodium) {
    return FoodItem.builder()
        .name(name)
        .category(cat)
        .caloriesPer100g(cal)
        .proteinPer100g(prot)
        .carbsPer100g(carb)
        .fatPer100g(fat)
        .sugarPer100g(sugar)
        .fiberPer100g(fiber)
        .sodiumMgPer100g(sodium)
        .build();
  }
}
