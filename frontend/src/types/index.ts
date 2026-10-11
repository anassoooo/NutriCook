export interface AuthResponse {
  userId: number
  token: string
  email: string
  role: string
}

export type Gender = 'MALE' | 'FEMALE' | 'OTHER' | 'PREFER_NOT_TO_SAY'

/** Options offered in forms. OTHER uses the midpoint BMR equation (BR-NRG-01). */
export const GENDER_OPTIONS: { value: Gender; label: string }[] = [
  { value: 'MALE', label: '♂ Male' },
  { value: 'FEMALE', label: '♀ Female' },
  { value: 'OTHER', label: 'Other' },
]

export type MealSlot = 'BREAKFAST' | 'MORNING_SNACK' | 'LUNCH' | 'AFTERNOON_SNACK' | 'DINNER'

/** One meal slot's share of the daily targets (computed by the API). */
export interface SlotTarget {
  slot: MealSlot
  share: number
  kcal: number
  proteinG: number
  carbsG: number
  fatG: number
  fiberG: number
}

/** Daily targets from GET /users/{id}/targets or embedded in the profile (BR-PRF-08). */
export interface Targets {
  kcal: number
  proteinG: number
  carbsG: number
  fatG: number
  fiberG: number
  bmr: number
  tdee: number
  floorApplied: boolean
  slots: SlotTarget[]
}

/** Why targets are not available yet, e.g. PROFILE_INCOMPLETE with details.missingFields. */
export interface TargetsError {
  code: string
  message: string
  details: { missingFields?: string[]; minAge?: number; maxAge?: number }
}

export interface UserProfile {
  id: number
  firstName: string | null
  lastName: string | null
  weightKg: number | null
  heightCm: number | null
  dateOfBirth: string | null
  gender: Gender | null
  activityLevel: ActivityLevel | null
  healthGoal: HealthGoal | null
  timezone: string
  healthConditions: HealthCondition[]
  dietaryRestrictions: DietaryRestriction[]
  targets: Targets | null
  targetsError: TargetsError | null
  /** "NOT_MEDICAL_ADVICE" when the user has a health condition (BR-SAF-08) */
  disclaimer: string | null
}

export type ActivityLevel =
  | 'SEDENTARY'
  | 'LIGHTLY_ACTIVE'
  | 'MODERATELY_ACTIVE'
  | 'VERY_ACTIVE'
  | 'EXTRA_ACTIVE'

export type HealthGoal =
  | 'LOSE_WEIGHT'
  | 'GAIN_MUSCLE'
  | 'IMPROVE_HEALTH'
  | 'MAINTAIN_WEIGHT'

export interface HealthCondition {
  id: number
  name: string
  description: string | null
  sugarLimitPerMealG: number | null
  fatLimitPerMealG: number | null
  sodiumLimitPerDayMg: number | null
}

export interface DietaryRestriction {
  id: number
  name: string
  type: 'PREFERENCE' | 'INTOLERANCE' | 'ALLERGY' | 'RELIGIOUS'
  description: string | null
}


export type FoodRole = 'PROTEIN' | 'CARB' | 'PRODUCE' | 'FAT'

/** One food in a meal, with its nutrition at generation time (BR-LIF-05). */
export interface MealComponent {
  foodId: number
  foodName: string
  role: FoodRole
  grams: number
  kcal: number
  proteinG: number
  carbsG: number
  fatG: number
}

export interface Meal {
  id: number
  name: string
  mealType: string
  calories: number
  proteinG: number
  carbsG: number
  fatG: number
  sugarG: number
  fiberG: number
  sodiumMg: number | null
  sortOrder: number
  aiSuggestion: string | null
  /** Checked off by the user (BR-PRG-05) */
  completed: boolean
  components: MealComponent[]
}

/** A non-blocking issue reported by the generator (BR-CMP-05). */
export interface PlanWarning {
  code: 'TARGET_DEVIATION' | 'LOW_PROTEIN' | 'AI_UNAVAILABLE'
  message: string
}

export interface DietPlan {
  id: number
  title: string
  totalCalories: number
  totalProteinG: number
  totalCarbsG: number
  totalFatG: number
  totalSugarG: number
  totalFiberG: number | null
  totalSodiumMg: number | null
  validForDate: string
  generatedAt: string
  status: 'ACTIVE' | 'SUPERSEDED' | 'ARCHIVED'
  /** 1 for the day's first plan, 2 after one regeneration, ... */
  generationSeq: number
  aiExplanation: string | null
  /** The AI text is written after the plan is saved (BR-LIF-07) */
  aiStatus: 'PENDING' | 'READY' | 'UNAVAILABLE'
  warnings: PlanWarning[]
  disclaimer?: string | null
  meals: Meal[]
}

export interface ProgressEntry {
  id: number
  date: string
  weightKg: number | null
  /** From checked meals, unless `caloriesOverridden` (BR-PRG-05) */
  caloriesConsumed: number | null
  /** true when the user typed the calories themselves */
  caloriesOverridden: boolean
  /** Derived: the number of meals checked off */
  mealsCompleted: number
  /** The target this day is judged against (BR-PRG-06) */
  targetCalories: number | null
  waterMl: number | null
  exerciseMinutes: number | null
  notes: string | null
}

export type AchievementType =
  | 'LOGGING_STREAK'
  | 'PLANS_GENERATED'
  | 'CALORIE_GOAL'
  | 'HYDRATION'
  | 'WEIGHT_MILESTONE'

/** An achievement and the user's standing on it (BR-ACH). The server computes every number. */
export interface Achievement {
  id: number
  name: string
  /** States the exact rule */
  description: string
  iconName: string | null
  type: AchievementType
  threshold: number
  /** In the threshold's unit: days, plan days or kg. Absent for newly awarded ones */
  currentValue: number | null
  earned: boolean
  earnedAt: string | null
}

/** A catalog food as administered (BR-CAT-01). Values are per 100 g. */
export interface CatalogFood {
  id: number
  name: string
  category: string | null
  role: FoodRole
  state: 'RAW' | 'COOKED' | 'DRY'
  caloriesPer100g: number
  proteinPer100g: number
  carbsPer100g: number
  fatPer100g: number
  sugarPer100g: number
  fiberPer100g: number
  sodiumMgPer100g: number
  portionMinG: number
  portionMaxG: number
  portionStepG: number
  defaultPortionG: number
  allergens: string[]
  dietFlags: string[]
  suitableSlots: string[]
  meat: boolean
  source: string | null
  /** SEED foods change through foods.csv; only ADMIN foods are editable here */
  origin: 'SEED' | 'ADMIN'
  active: boolean
}

export interface Page<T> {
  items: T[]
  page: number
  size: number
  totalItems: number
  totalPages: number
}

export interface LocationDTO {
  name: string
  type: string
  latitude: number
  longitude: number
  address: string
  osmNodeId: number
}
