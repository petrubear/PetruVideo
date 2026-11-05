# Tasks: Text Rotation, Shadow and Spacing

- [X] T001 Record on a device a fingerprint of today's rendering of default text clips, as a test in `core/media/src/androidTest/.../text/TextRendererTest.kt`
- [X] T002 [US1-3] Test then add `TextShadow` and the new `TextStyle` fields, `normalized()` and `snapRotation` in `core/model/.../Text.kt` (`TextStyleTest.kt`)
- [X] T003 [US1-3] Test then add the pure geometry helpers and draw rotation, shadow and spacing in `TextRenderer.kt` (`TextGeometryTest.kt`)
- [X] T004 Check on a device that the fingerprints still match and that rotation, shadow and spacing change the rendering as expected
- [X] T005 Add the columns, mappers, schema 25 and `Migration24To25Test.kt` in `core/data`
- [X] T006 [US1-3] Add the controls to `TextStyleControls` in `feature/editor` and the English and Spanish strings
- [X] T007 Add Style panel screenshots (dark, light, Spanish) and run detekt, lint, unit tests, Paparazzi verify, assembleRelease
- [X] T008 CHANGELOG, version 0.10.0, commit
