# Research: Theme Setting

- **Decision**: keep the choice in the existing settings preferences. **Why**: it is the app's only settings store, read synchronously at creation, which gives the correct first frame. **Rejected**: DataStore (asynchronous first read would flash the wrong look), per-activity night mode (restarts the activity, which loses state).
- **Decision**: decide dark or light in Compose state in `MainActivity`. **Why**: the activity already declares `uiMode` in `configChanges`, so no recreation happens and an in-app change is only a recomposition. 
- **Decision**: reuse `SegmentedControl` and `enumLabel`. **Why**: same pattern as default export quality; no new component.
