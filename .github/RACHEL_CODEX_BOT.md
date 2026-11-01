# Rachel Codex Bot

The reusable `Rachel Codex Bot` workflow turns a task patch into a signed pull request created by the repository's GitHub App. It is task-agnostic: issue numbers, source files, titles, and patch contents are supplied per task, not hard-coded in the workflow.

## Required repository secret

- `RACHEL_CODEX_APP_PRIVATE_KEY`: the PEM private key for GitHub App ID `5563370`.

The workflow requests a short-lived installation token scoped to `rachel-ylcs/ylcs-kmp` with Contents and Pull requests write permissions. It checks the installation ID and never prints the private key or token.

## Submit a task

1. Create a temporary branch named `codex/tasks/<task-slug>` from the intended base branch.
2. Create one file at `.codex/task.json` on that branch. The single-file commit triggers the workflow.
3. The workflow applies the base64-encoded unified diff to the selected base branch, then creates or updates the PR branch specified in the manifest.
4. After a successful push-triggered run, the temporary `codex/tasks/<task-slug>` branch is deleted. The PR branch remains.

Example manifest:

```json
{
  "base_branch": "main",
  "pr_branch": "codex/example-task",
  "title": "feat: describe the task",
  "body": "## Summary\n- Describe the change\n\nCloses #123",
  "commit_message": "feat: describe the task",
  "patch_base64": "<base64-encoded unified git diff>"
}
```

Use a stable `pr_branch` when updating the same task. The workflow reuses that branch and updates its existing PR instead of creating a duplicate. Use a new `pr_branch` for a separate task.

## Manual run

After the workflow is on the default branch, it can also be started from **Actions → Rachel Codex Bot → Run workflow**. Supply the existing `codex/tasks/<task-slug>` branch; optionally override the base branch. Manual runs do not delete the task branch.

The workflow only accepts task branches under `codex/tasks/` and PR branches under `codex/`. It is not triggered by ordinary PR branches, its own PR branch, or changes to workflow files.
