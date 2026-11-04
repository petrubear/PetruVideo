# Data Model: Project Card Preview

No database change. The existing nullable `Project.thumbnailRef` (path string) is used as it is.

## Project picture

| Field | Meaning |
|---|---|
| `Project.thumbnailRef` | Absolute path of the picture file, or null when there is none. |
| File `files/project-thumbs/<projectId>-<key>.png` | The picture; `<key>` is a short hash of the first clip's media id and trimmed start, so it changes when the first clip changes. |

### Rules

- At most one picture file per project; writing a new one deletes the old one.
- `thumbnailRef` is null when the project has no clip, the first clip's media is missing or unreadable, or the picture was not made yet.
- The file is removed when the project is deleted; a duplicate gets its own file or none.
- If the referenced file does not exist, the view shows the gradient and the backfill makes it again.

### State transitions

```text
no picture --(first clip exists, backfill or editor refresh)--> picture
picture --(first clip changes)--> new picture (old file deleted)
picture --(all clips removed or media missing)--> no picture (file deleted)
picture --(project deleted)--> file deleted
```

## Backfill queue (screen state only, not stored)

- Projects with `clipCount > 0` and `thumbnailRef == null`, processed one at a time.
- A set of project ids that failed in this session, so they are not retried until the app restarts.
