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
    // Fruits
    foodRepo.save(fi("Apple", "fruit", 52, 0.3, 14.0, 0.2, 10.4, 2.4, 1.0));
    foodRepo.save(fi("Banana", "fruit", 89, 1.1, 23.0, 0.3, 12.2, 2.6, 1.0));
    foodRepo.save(fi("Avocado", "fruit", 160, 2.0, 9.0, 15.0, 0.7, 6.7, 7.0));
    // Vegetables
    foodRepo.save(fi("Spinach", "vegetable", 23, 2.9, 3.6, 0.4, 0.4, 2.2, 79.0));
    foodRepo.save(fi("Sweet Potato", "vegetable", 86, 1.6, 20.0, 0.1, 4.2, 3.0, 55.0));
    // Grains
    foodRepo.save(fi("Oatmeal", "grain", 389, 16.9, 66.0, 6.9, 0.0, 10.6, 2.0));
    foodRepo.save(fi("Brown Rice", "grain", 216, 4.5, 45.0, 1.8, 0.7, 3.5, 7.0));
    foodRepo.save(fi("Quinoa", "grain", 368, 14.1, 64.0, 6.1, 0.0, 7.0, 5.0));
    // Legumes
    foodRepo.save(fi("Lentils", "legume", 116, 9.0, 20.0, 0.4, 1.8, 7.9, 2.0));
    foodRepo.save(fi("Black Beans", "legume", 341, 21.6, 62.0, 1.4, 2.1, 15.5, 5.0));
    foodRepo.save(fi("Tofu", "legume", 76, 8.1, 1.9, 4.8, 0.0, 0.3, 7.0));
    // Nuts & Seeds
    foodRepo.save(fi("Almonds", "nut", 579, 21.2, 22.0, 49.9, 4.4, 12.5, 1.0));
    // Dairy
    foodRepo.save(fi("Greek Yogurt", "dairy", 59, 10.2, 3.6, 0.4, 3.2, 0.0, 46.0));
    foodRepo.save(fi("Cottage Cheese", "dairy", 98, 11.1, 3.4, 4.3, 2.7, 0.0, 364.0));
    // Meat & Fish
    foodRepo.save(fi("Chicken Breast", "meat", 165, 31.0, 0.0, 3.6, 0.0, 0.0, 74.0));
    foodRepo.save(fi("Salmon", "fish", 208, 20.0, 0.0, 13.0, 0.0, 0.0, 59.0));
    foodRepo.save(fi("Tuna (canned)", "fish", 116, 25.5, 0.0, 1.0, 0.0, 0.0, 337.0));
    foodRepo.save(fi("Eggs", "meat", 155, 13.0, 1.1, 11.0, 0.0, 0.0, 124.0));
    // Healthy fats
    foodRepo.save(fi("Olive Oil", "fat", 884, 0.0, 0.0, 100.0, 0.0, 0.0, 2.0));
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
