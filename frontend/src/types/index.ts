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

export interface MealFoodItem {
  foodItemId: number
  foodItemName: string
  quantityG: number
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
  sortOrder: number
  aiSuggestion: string | null
  mealFoodItems: MealFoodItem[]
}

export interface DietPlan {
  id: number
  title: string
  totalCalories: number
  totalProteinG: number
  totalCarbsG: number
  totalFatG: number
  totalSugarG: number
  validForDate: string
  generatedAt: string
  status: 'ACTIVE' | 'ARCHIVED'
  aiExplanation: string | null
  disclaimer?: string | null
  meals: Meal[]
}

export interface ProgressEntry {
  id: number
  date: string
  weightKg: number | null
  caloriesConsumed: number | null
  waterMl: number | null
  exerciseMinutes: number | null
  notes: string | null
}

export interface Achievement {
  id: number
  name: string
  description: string
  type: string
  iconUrl: string | null
  earnedAt: string
}

export interface LocationDTO {
  name: string
  type: string
  latitude: number
  longitude: number
  address: string
  osmNodeId: number
}
