package com.nutricook.entity.enums;

/** Where a catalog food comes from (BR-CAT-01/05). */
public enum FoodOrigin {
  /** db/catalog/foods.csv, applied by R__food_catalog.sql: values change through the file. */
  SEED,
  /** Created through the admin API: fully editable there. */
  ADMIN
}
