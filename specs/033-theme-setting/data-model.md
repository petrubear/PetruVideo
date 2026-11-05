# Data Model: Theme Setting

- **ThemeChoice**: `System` (default) | `Light` | `Dark`.
- **AppSettings**: gains `theme: ThemeChoice = System`. Stored as the enum name under key `theme_choice`; missing or unknown means `System`.
- **Rule**: `isDark(choice, systemDark) = when (choice) { System -> systemDark; Light -> false; Dark -> true }`.
