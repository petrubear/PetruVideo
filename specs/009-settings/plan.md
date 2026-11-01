# Implementation Plan: Settings

Summary: `AppSettings` (default export quality, lower preview) in `core:model`; `SettingsRepository` in `core:data` backed by SharedPreferences with a `StateFlow`; the pressure monitor gets `setForcedReduced` so `PreviewQuality` is Reduced while the switch is on or pressure is not Normal; the application collects the setting into the monitor; `ExportViewModel` starts from the default quality; `feature:projects` gets `SettingsViewModel`, `SettingsScreen`, `SettingsRoute`, a header button effect and a nav route.

Constitution: all PASS (native, non-destructive, simple, test-first logic, modular, on-device, adaptive layouts).

Risks: forced reduced vs pressure interplay (R1: quality = Reduced when either holds, tested); preference writes off the main thread (R2: apply() is asynchronous).

Tests: monitor unit tests, settings repository test on device, view model tests, export default tests, Compose, accessibility, goldens.
