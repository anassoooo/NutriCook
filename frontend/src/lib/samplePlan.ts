import type { Meal, MealComponent } from '../types'

/**
 * An illustrative day, used only on the public pages (landing, sign-in). Values are per portion
 * and follow the catalog's per-100 g data; real plans always come from the API.
 */
function c(foodId: number, foodName: string, role: MealComponent['role'], grams: number, kcal: number): MealComponent {
  return { foodId, foodName, role, grams, kcal, proteinG: 0, carbsG: 0, fatG: 0 }
}

function meal(id: number, name: string, mealType: string, components: MealComponent[], completed: boolean): Meal {
  const calories = Math.round(components.reduce((s, x) => s + x.kcal, 0))
  return {
    id,
    name,
    mealType,
    calories,
    proteinG: 0,
    carbsG: 0,
    fatG: 0,
    sugarG: 0,
    fiberG: 0,
    sodiumMg: 0,
    sortOrder: id,
    aiSuggestion: null,
    completed,
    components,
  }
}

export const SAMPLE_MEALS: Meal[] = [
  meal(
    1,
    'Breakfast',
    'BREAKFAST',
    [
      c(1, 'Eggs', 'PROTEIN', 100, 155),
      c(2, 'Whole wheat bread', 'CARB', 80, 198),
      c(3, 'Orange', 'PRODUCE', 150, 71),
      c(4, 'Olive oil', 'FAT', 10, 88),
    ],
    true,
  ),
  meal(
    2,
    'Morning snack',
    'MORNING_SNACK',
    [c(5, 'Greek yogurt', 'PROTEIN', 150, 146), c(6, 'Dates', 'PRODUCE', 40, 113)],
    true,
  ),
  meal(
    3,
    'Lunch',
    'LUNCH',
    [
      c(7, 'Chicken breast', 'PROTEIN', 140, 231),
      c(8, 'Couscous', 'CARB', 200, 224),
      c(9, 'Tomato and cucumber', 'PRODUCE', 200, 32),
      c(4, 'Olive oil', 'FAT', 12, 106),
    ],
    false,
  ),
  meal(
    4,
    'Afternoon snack',
    'AFTERNOON_SNACK',
    [c(10, 'Chickpeas', 'PROTEIN', 100, 164), c(11, 'Apple', 'PRODUCE', 150, 78)],
    false,
  ),
  meal(
    5,
    'Dinner',
    'DINNER',
    [
      c(12, 'Sea bass', 'PROTEIN', 150, 186),
      c(13, 'Brown rice', 'CARB', 180, 202),
      c(14, 'Zucchini', 'PRODUCE', 200, 34),
      c(15, 'Almonds', 'FAT', 15, 87),
    ],
    false,
  ),
]

export const SAMPLE_TOTAL = SAMPLE_MEALS.reduce((s, m) => s + m.calories, 0)
export const SAMPLE_EATEN = SAMPLE_MEALS.filter((m) => m.completed).reduce((s, m) => s + m.calories, 0)
