# PetruVideo

## Asking the user what to do next

The user follows sessions from mobile, where the next-step box (`wrap_up` with `ask` and `actions`) is not supported but `AskUserQuestion` is.

- At the end of a turn, ask any next-step decision with the `AskUserQuestion` tool, recommended option first.
- Still call `wrap_up` if the app requires it, but with a recap only: no `ask`, no `actions`.
- Use `AskUserQuestion` for any other decision that needs the user's input as well.
