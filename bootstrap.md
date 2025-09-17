### Pieq API Template Bootstrapping

This guide explains what the bootstrapping scripts do and how to use them to turn this template into a new service with your names, packages, and modules.

- Scripts: `bootstrap.sh` (macOS/Linux) and `bootstrap.ps1` (Windows PowerShell)
- Run from the template repository root (must be the root containing `pieq-api`, `pieq-application`, `pieq-client`)
- Target JDK: 21+

## What the scripts do (high level)
- Rename the three Gradle modules to match your service name:
  - `pieq-api` → `<service-name>-api`
  - `pieq-application` → `<service-name>-application`
  - `pieq-client` → `<service-name>-client`
- Set Gradle `group` to your value and rewrite inter-project Gradle dependencies to use the new module names
- Move Kotlin sources into the new package path `com.pieq.<package-segment>`
- Preserve any `com.pieq.core` imports/packages (never rewritten)
- Rename core classes:
  - `PieqApiApplication` → `<ClassPrefix>Application`
  - `PieqApiConfiguration` → `<ClassPrefix>Configuration`
- Rename feature classes (Windows only; macOS/Linux keeps the template `User` model):
  - `UserResource`/`UserService`/`UserDao`/`UserClient`/`User` → `<ModelName>…` (Windows)
- Align Kotlin files to match their `package` directories on disk
- Update Application main class FQCN in Gradle (`...ApplicationKt`)
- Update YAML (`name:` and module mentions) to reflect your service and package
- Update `Dockerfile` and `docker.local` to the new module names and jar filename
- Parameterize Docker builds with `MODULE_API`, `MODULE_APP`, `MODULE_CLIENT` so images work after renaming
- Print a summary of changes

## Platform differences
- macOS/Linux (`bootstrap.sh`): operates in-place in the current repo. Does not set `rootProject.name`, and keeps the `User` model (no model renaming flag).
- Windows (`bootstrap.ps1`): operates in-place in the current repo, sets `rootProject.name`, and supports optional model renaming via `-ModelName`.

## Prerequisites
- macOS/Linux: `git`, `rsync`, `perl`, `sed`
- Windows PowerShell: PowerShell 5+ (7+ recommended)
- JDK 21+ (for building/running the resulting project)

## Arguments
Concepts:
- `service name`: Used for module renames: `<service-name>-api|application|client`
- `package segment`: New package segment after `com.pieq.` → `com.pieq.<segment>`
- `class prefix`: Used for application/config classes: `<ClassPrefix>Application`, `<ClassPrefix>Configuration`
- Windows only: `repo name` sets `rootProject.name`; `model name` optionally renames the template model/classes (default `User`).

macOS/Linux (`bootstrap.sh`) flags:
- `--service-name <name>`
- `--package-segment <segment>`
- `--class-prefix <PascalCase>`

Windows PowerShell (`bootstrap.ps1`) parameters:
- `-RepoName <name>`
- `-ServiceName <name>`
- `-PackageSegment <segment>`
- `-ClassPrefix <PascalCase>`
- `-ModelName <PascalCase>` (optional; defaults to `User`)

## What gets changed (step-by-step)
1. Patch `settings.gradle.kts` (or `settings.gradle`):
   - Replace included module names to new `<service-name>-…` names (including `":…"` forms)
   - Windows only: set `rootProject.name = "<repo-name>"`
2. Rename module directories on disk:
   - `pieq-api` → `<service-name>-api`
   - `pieq-application` → `<service-name>-application`
   - `pieq-client` → `<service-name>-client`
3. Rewrite inter-project Gradle dependencies in all `build.gradle.kts`/`build.gradle`:
   - Any `project(":pieq-…")` → `project(":<service-name>-…")`
   - Ensure client depends on application, application depends on api
4. Set Gradle `group = "com.pieq.<package-segment>"` in all Gradle files; collapse accidental duplicates (e.g., `com.pieq.foo.foo`)
5. Move source packages on disk under `src/main/kotlin` and `src/test/kotlin`:
   - `com/pieq` → `com/pieq/<package-segment>`
6. Guarded package/import replacements in all `*.kt`, `*.kts`, `*.java`:
   - Replace `com.pieq` with `com.pieq.<package-segment>` while preserving `com.pieq.core`
7. Rename core application/config files and in-file class references:
   - `PieqApiApplication.kt` → `<ClassPrefix>Application.kt`
   - `PieqApiConfiguration.kt` → `<ClassPrefix>Configuration.kt`
8. Rename feature files and references (Resource/Service/Dao/Client/Model):
   - Windows: to use `<ModelName>`
   - macOS/Linux: keeps the template `User` model
9. Align Kotlin files to package-correct directories (move files to match `package` lines)
10. Update Gradle `mainClass`/manifest to `<pkg>.<ClassPrefix>ApplicationKt`
11. Update YAML files:
   - Replace old module names with new ones
   - Update `name: "<service-name>"`
   - Apply the same guarded package rewrite
12. Update Docker builds:
   - `Dockerfile` and `docker.local` are updated to use your new module names
   - Both files use build args `MODULE_API`, `MODULE_APP`, `MODULE_CLIENT` (defaults: `pieq-api`, `pieq-application`, `pieq-client`)
   - Jar path is normalized to `${MODULE_APP}.jar` in `build/libs/`
13. Print a summary of groups and inter-project dependencies

## Usage

### macOS/Linux
1. Make the script executable (first run only):
```bash
chmod +x ./bootstrap.sh
```
2. Run from the template root with your values:
```bash
./bootstrap.sh \
  --service-name user-management \
  --package-segment usermanagement \
  --class-prefix UserManagementApi
```
3. Verify, build, and run:
```bash
./gradlew clean build
(cd user-management-application && ./gradlew installDist && ./build/install/user-management-application/bin/user-management-application server src/main/resources/config_dev.yml)
```

### Windows PowerShell
1. Run from the template root:
```powershell
./bootstrap.ps1 -RepoName "my-user-mgmt" -ServiceName "user-management" -PackageSegment "usermanagement" -ClassPrefix "UserManagementApi" -ModelName "Account"
```
2. Follow the printed summary, then build and run:
```powershell
.\gradlew clean build
.\gradlew :user-management-application:installDist
.\user-management-application\build\install\user-management-application\bin\user-management-application server user-management-application\src\main\resources\config_dev.yml
```

## Docker integration
- After bootstrapping, `Dockerfile` and `docker.local` build successfully without manual edits.
- Both files accept module-name build args:
  - `MODULE_API` (default `pieq-api`)
  - `MODULE_APP` (default `pieq-application`)
  - `MODULE_CLIENT` (default `pieq-client`)
- The application jar copied during the image build is `${MODULE_APP}.jar` from `${MODULE_APP}/build/libs/`.
- Example override at build time:
```bash
docker build --build-arg MODULE_APP=user-management-application -t user-mgmt:latest .
```

## Verifying the result
- Confirm module folders were renamed to `<service-name>-api|application|client`
- Search for `com.pieq.core` remains untouched; other `com.pieq` moved to `com.pieq.<segment>`
- Ensure `build.gradle.kts` in application sets `mainClass`/manifest to `<ClassPrefix>ApplicationKt`
- Build succeeds: `./gradlew clean build`
- App starts using `config_dev.yml`
- Docker builds succeed with default or overridden module args

## Tips & troubleshooting
- Missing tools (macOS/Linux): install `git`, `rsync`, `perl`, `sed`
- Run on a fresh copy of the template. If anything fails mid-way, revert your working tree or reclone
- The scripts are intended to be run once; avoid running repeatedly on the same tree
- If your terminal is `zsh`, the script handles `sed -i ''` for macOS already
- If you later change `<ClassPrefix>` or `<package-segment>`, re-run the relevant rename/move steps manually or re-bootstrap from a fresh template

## FAQ
- Why preserve `com.pieq.core`? — Those packages are shared core libraries and should not be rewritten into your service’s package
- Do these scripts clone a repo? — No; both scripts operate in-place from the repo root
- Can I choose a different base package than `com.pieq`? — The scripts assume `com.pieq.<segment>`; customizing beyond that requires manual edits

## Quick reference
- macOS/Linux run:
```bash
./bootstrap.sh --service-name <svc> --package-segment <seg> --class-prefix <Pascal>
```
- Windows run:
```powershell
./bootstrap.ps1 -RepoName <repo> -ServiceName <svc> -PackageSegment <seg> -ClassPrefix <Pascal> [-ModelName <Pascal>]
``` 