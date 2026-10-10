package com.nutricook.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import java.util.Random;

/**
 * Layering rules from Constitution IV, enforced on every build.
 *
 * <p>The domain ({@code com.nutricook.domain..}) is pure Java: no framework, no persistence, no
 * clock, no unseeded randomness. That is what makes the business rules unit- and property-testable.
 */
@AnalyzeClasses(packages = "com.nutricook", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

  @ArchTest
  static final ArchRule domainIsFrameworkFree =
      noClasses()
          .that()
          .resideInAPackage("com.nutricook.domain..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "org.springframework..",
              "jakarta.persistence..",
              "org.hibernate..",
              "com.nutricook.entity..",
              "com.nutricook.repository..",
              "com.nutricook.service..",
              "com.nutricook.controller..",
              "com.nutricook.config..",
              "com.nutricook.ai..")
          .because("business rules must be testable without Spring, a database or the AI");

  private static final DescribedPredicate<JavaMethodCall> SYSTEM_CLOCK =
      DescribedPredicate.describe(
          "read the system clock",
          call ->
              (call.getName().equals("now")
                      && call.getTargetOwner().getPackageName().equals("java.time"))
                  || (call.getName().equals("currentTimeMillis")
                      && call.getTargetOwner().isEquivalentTo(System.class)));

  @ArchTest
  static final ArchRule BR_TIM_03_noNowInDomain =
      noClasses()
          .that()
          .resideInAPackage("com.nutricook.domain..")
          .should()
          .callMethodWhere(SYSTEM_CLOCK)
          .because("time must come from an injected java.time.Clock (BR-TIM-03)");

  @ArchTest
  static final ArchRule BR_CMP_07_noUnseededRandomnessInDomain =
      noClasses()
          .that()
          .resideInAPackage("com.nutricook.domain..")
          .should()
          .callConstructor(Random.class)
          .orShould()
          .callMethod(Math.class, "random")
          .because("plan generation must be reproducible from its seed (BR-CMP-07)");

  private static final DescribedPredicate<JavaMethodCall> SAVES_A_PLAN =
      DescribedPredicate.describe(
          "save a DietPlan",
          call ->
              call.getTargetOwner().getName().equals("com.nutricook.repository.DietPlanRepository")
                  && call.getName().startsWith("save"));

  @ArchTest
  static final ArchRule BR_SAF_05_onlyPlanCommandServiceSavesPlans =
      noClasses()
          .that()
          .doNotHaveSimpleName("PlanCommandService")
          .should()
          .callMethodWhere(SAVES_A_PLAN)
          .because("every plan write must pass the safety validator first (BR-SAF-05, plan D3)");

  @ArchTest
  static final ArchRule controllersDoNotUseRepositories =
      noClasses()
          .that()
          .resideInAPackage("com.nutricook.controller..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("com.nutricook.repository..")
          .because("controllers stay thin: validate, delegate to a service, map");
}
