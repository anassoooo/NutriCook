export interface AuthResponse {
  userId: number
  token: string
  email: string
  role: string
}

export interface UserProfile {
  id: number
  firstName: string | null
  lastName: string | null
  weightKg: number | null
  heightCm: number | null
  dateOfBirth: string | null
  gender: 'MALE' | 'FEMALE' | null
  activityLevel: ActivityLevel | null
  healthGoal: HealthGoal | null
  healthConditions: HealthCondition[]
  dietaryRestrictions: DietaryRestriction[]
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
}

export interface DietaryRestriction {
  id: number
  name: string
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
