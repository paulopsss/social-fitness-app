# Git workflow for Fitly

This guide defines how the team proposes, reviews, validates, and releases changes to Fitly, a Java 17+ Spring Boot application built with Maven. The workflow is trunk based: `main` is the production-ready branch; all changes reach it through reviewed pull requests.

## Branches and names

- `main`: protected, deployable code. No direct pushes or force pushes.
- `feature/<short-kebab-case>`: product work, for example `feature/workout-history`.
- `fix/<short-kebab-case>`: defects, for example `fix/session-timeout`.
- `docs/<short-kebab-case>`: documentation-only changes.
- `ci/<short-kebab-case>`: build and automation work.
- `hotfix/<short-kebab-case>`: urgent production corrections; still use a pull request and required checks.

Start each branch from an up-to-date `main`. Keep branches focused and short lived. Rebase or merge the latest `main` before requesting final review when needed; resolve conflicts on the feature branch.

## Commit messages

Use Conventional Commits: `<type>(optional-scope): imperative summary`. Keep the subject under 72 characters. Common types are `feat`, `fix`, `docs`, `test`, `refactor`, `build`, and `ci`.

Examples:

- `feat(workouts): add workout history endpoint`
- `fix(auth): reject expired sessions`
- `ci: add Maven pull request checks`

Use a body to explain motivation or tradeoffs. Reference issues with `Refs #123` where applicable. Do not include secrets or generated build output.

## Pull request process

1. Push the branch and open a PR targeting `main` using the repository PR template.
2. Explain the change, its motivation, and user-visible impact. Link the issue, list test evidence, and disclose risks or migration needs.
3. Request at least one review from an eligible teammate who did not author the change. CODEOWNERS can route review requests after the example ownership team is replaced with the actual GitHub team.
4. The reviewer checks correctness, readability, tests, security, and scope. Put actionable comments on specific lines; the author resolves them and responds before re-requesting review.
5. Wait for the required `build-and-quality` check. It runs Checkstyle static analysis and `mvn verify` (including JUnit tests and Maven packaging) on every PR to `main`.
6. Resolve all conversations, obtain approval, and merge using **Squash and merge**. Use the PR title as the resulting Conventional Commit subject. Delete the feature branch after merge.

Do not self-approve. Do not merge with failing or skipped required checks. If a check is flaky, rerun it and investigate before merging; do not bypass the rule.

## Merge and release policy

Only reviewed, green PRs enter `main`. Squash merges keep the mainline history readable. A merge to `main` is the release candidate and must remain deployable. Production deployment is a separate, authorized release action; this CI workflow builds and validates but does not deploy because no hosting target or deployment credentials were supplied. For a production release, tag the approved main commit as `vMAJOR.MINOR.PATCH` and use a separately protected deployment workflow/environment with required approvals and secrets.

## Required repository protection

Configure a GitHub ruleset or branch protection rule for `main` in repository Settings. Recommended settings:

- Require a pull request before merging and **1 approving review** from someone other than the author.
- Dismiss stale approvals when new commits are pushed; require approval of the most recent reviewable push if available.
- Require the `build-and-quality` status check (from the `CI` workflow).
- Require conversation resolution before merging.
- Block force pushes and branch deletion; do not allow bypass actors for routine contributors.
- Restrict direct pushes to `main` by making PR review mandatory and keeping write access limited to maintainers.
- Select squash merge as the only merge method in repository settings.

GitHub branch protection is hosted repository state and cannot be activated by committing a file. Apply these settings in the repository UI or through an authorized GitHub API session, then verify a test PR cannot merge without approval and CI.

## CI behavior

`.github/workflows/ci.yml` runs on PRs into `main`, pushes to `main`, and manual dispatch. It uses Java 17, Maven dependency caching, Checkstyle, JUnit tests, and Maven `verify` packaging. The required check name to select in branch protection is `build-and-quality`.

## Pull request evidence record

The requested evidence set is three real PRs, each with a description, teammate review comments/approval, and a passing CI run before merge. Create those PRs only after this workflow is installed and branch protection is active. Save screenshots that show the PR number and title, review activity, and green check result. This prepared package does not claim PRs, reviews, merges, or GitHub checks that have not been performed. Record links and screenshots here after those real GitHub actions are complete.

| PR | Title / URL | Teammate review | CI result | Screenshot |
|---|---|---|---|---|
| 1 | Pending live GitHub access | Pending | Pending | Pending |
| 2 | Pending live GitHub access | Pending | Pending | Pending |
| 3 | Pending live GitHub access | Pending | Pending | Pending |

## Workflow diagram

See `docs/git-workflow.mmd` for the source diagram. The PDF deliverable includes a rendered version.
