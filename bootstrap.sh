#!/usr/bin/env bash
set -Eeuo pipefail

# ===================== Logging helpers =====================
ts()  { date +"%Y-%m-%d %H:%M:%S"; }
log() { echo "[$(ts)] $*"; }
ok()  { echo "[$(ts)] ✅ $*"; }
warn(){ echo "[$(ts)] ⚠️  $*" >&2; }
err() { echo "[$(ts)] ❌ $*" >&2; }
step(){ echo; echo "[$(ts)] ── $*"; }

trap 'err "Error on line $LINENO. Last: $BASH_COMMAND"; exit 1' ERR

# ===================== prerequisites =====================
need(){ command -v "$1" >/dev/null || { err "Missing required tool: $1"; exit 1; }; }
need git; need rsync; need perl; need sed

# ===================== template constants =====================
TEMPLATE_REPO_DEFAULT="https://github.com/pieq-ai/pieq-api-template"

CUR_SUB_API="pieq-api"
CUR_SUB_APP="pieq-application"
CUR_SUB_CLIENT="pieq-client"
CUR_BASE_PKG="com.pieq"

CUR_APP_CLASS="PieqApiApplication"
CUR_CFG_CLASS="PieqApiConfiguration"
CUR_MODEL_CLASS="User"
CUR_RESOURCE_CLASS="UserResource"
CUR_SERVICE_CLASS="UserService"
CUR_DAO_CLASS="UserDao"
CUR_CLIENT_CLASS="UserClient"

usage() {
  cat <<EOF
Usage:
  ./bootstrap.sh --service-name <name> --package-segment <segment> \
                 --class-prefix <Pascal>

Notes:
- Run this script from the repository root that contains ${CUR_SUB_API}, ${CUR_SUB_APP}, ${CUR_SUB_CLIENT}
- Keeps com.pieq.core imports/packages untouched
- Fixes Gradle groups (no duplicates), inter-project deps (uses project(":…")), and moves Kotlin files to match their package path
- Adds rich, timestamped logs for debugging
EOF
  exit 1
}

# ===================== args =====================
SERVICE_NAME=""
PKG_SEGMENT=""
CLASS_PREFIX=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --service-name) SERVICE_NAME="$2"; shift 2 ;;
    --package-segment) PKG_SEGMENT="$2"; shift 2 ;;
    --class-prefix) CLASS_PREFIX="$2"; shift 2 ;;
    -h|--help) usage ;;
    *) err "Unknown arg: $1"; usage ;;
  esac
done

[[ -z "$SERVICE_NAME" ]] && read -rp "Service name (e.g. user-management): " SERVICE_NAME
[[ -z "$PKG_SEGMENT" ]]  && read -rp "Package segment after com.pieq. (e.g. usermanagement): " PKG_SEGMENT
[[ -z "$CLASS_PREFIX" ]] && read -rp "Class prefix (e.g. UserManagementApi): " CLASS_PREFIX

# ===================== repo root validation =====================
step "Validating repository root"
if ! [[ -d "$CUR_SUB_API" && -d "$CUR_SUB_APP" && -d "$CUR_SUB_CLIENT" ]]; then
  err "Run this script at the repo root containing '$CUR_SUB_API', '$CUR_SUB_APP', '$CUR_SUB_CLIENT'"
  exit 1
fi

# ===================== derive names & paths =====================
NEW_API="${SERVICE_NAME}-api"
NEW_APP="${SERVICE_NAME}-application"
NEW_CLIENT="${SERVICE_NAME}-client"
NEW_GROUP="com.pieq.${PKG_SEGMENT}"

case "$NEW_GROUP" in com.pieq.*) ;; *) err "Package must start with com.pieq.<segment>"; exit 1 ;; esac

step "Planned layout"
log "Repository root: $(pwd)"
log "Modules       : $NEW_API, $NEW_APP, $NEW_CLIENT"
log "Gradle group  : $NEW_GROUP"
log "Class prefix  : ${CLASS_PREFIX} (…Application / …Configuration)"
log "Model         : ${CUR_MODEL_CLASS}"

# ===================== settings.gradle(.kts) =====================
SETTINGS=$(ls -1 settings.gradle.kts settings.gradle 2>/dev/null | head -n1 || true)
[[ -n "$SETTINGS" ]] || { err "settings.gradle(.kts) not found in repo root"; exit 1; }

step "Patching $SETTINGS"
log "Renaming includes: $CUR_SUB_API->$NEW_API, $CUR_SUB_APP->$NEW_APP, $CUR_SUB_CLIENT->$NEW_CLIENT"
perl -pi -e "s/\\b${CUR_SUB_API}\\b/${NEW_API}/g; s/\\b${CUR_SUB_APP}\\b/${NEW_APP}/g; s/\\b${CUR_SUB_CLIENT}\\b/${NEW_CLIENT}/g" "$SETTINGS"
perl -pi -e "s/:${CUR_SUB_API}/:${NEW_API}/g; s/:${CUR_SUB_APP}/:${NEW_APP}/g; s/:${CUR_SUB_CLIENT}/:${NEW_CLIENT}/g" "$SETTINGS"
ok "Patched $SETTINGS"

# ===================== rename module dirs =====================
step "Renaming module directories"
for pair in "$CUR_SUB_API:$NEW_API" "$CUR_SUB_APP:$NEW_APP" "$CUR_SUB_CLIENT:$NEW_CLIENT"; do
  from="${pair%%:*}"; to="${pair##*:}"
  if [[ -d "$from" ]]; then
    log "mv $from -> $to"
    mv "$from" "$to"
  else
    warn "Missing module dir: $from (skipped)"
  fi
done
ok "Module directories renamed"

# ===================== Gradle deps rewrite =====================
step "Rewriting inter-project Gradle dependencies"
count_before=$(grep -R "project(" -n . | wc -l | tr -d ' ')
find . -name "build.gradle.kts" -o -name "build.gradle" -print0 | xargs -0 perl -pi -e "
  s/project\([\"']:${CUR_SUB_API}[\"']\)/project(\":$NEW_API\")/g;
  s/project\([\"']:${CUR_SUB_APP}[\"']\)/project(\":$NEW_APP\")/g;
  s/project\([\"']:${CUR_SUB_CLIENT}[\"']\)/project(\":$NEW_CLIENT\")/g;
"
# Enforce client -> application, application -> api explicitly (always with double quotes)
if [[ -f "$NEW_CLIENT/build.gradle.kts" ]]; then
  log "Client deps: -> :$NEW_APP"
  perl -pi -e "s/project\([\"']:${SERVICE_NAME}-application[\"']\)/project(\":$NEW_APP\")/g; s/project\([\"']:${CUR_SUB_APP}[\"']\)/project(\":$NEW_APP\")/g" "$NEW_CLIENT/build.gradle.kts"
fi
if [[ -f "$NEW_APP/build.gradle.kts" ]]; then
  log "Application deps: -> :$NEW_API"
  perl -pi -e "s/project\([\"']:${SERVICE_NAME}-api[\"']\)/project(\":$NEW_API\")/g; s/project\([\"']:${CUR_SUB_API}[\"']\)/project(\":$NEW_API\")/g" "$NEW_APP/build.gradle.kts"
fi
count_after=$(grep -R "project(" -n . | wc -l | tr -d ' ')
ok "Gradle dependencies rewritten (lines with project(): $count_before -> $count_after)"

# ===================== Gradle group everywhere =====================
step "Setting Gradle group = $NEW_GROUP (and collapsing duplicates)"
files_changed=0
while IFS= read -r -d '' f; do
  perl -pi -e "s/^[[:space:]]*group[[:space:]]*=.*/group = \"$NEW_GROUP\"/g" "$f" && ((files_changed++)) || true
done < <(find . -name "build.gradle.kts" -o -name "build.gradle" -print0)
log "Updated group in $files_changed Gradle files"

# Collapse any accidental com.pieq.<seg>.<seg> anywhere in Gradle
find . -name "build.gradle.kts" -o -name "build.gradle" -print0 \
| xargs -0 perl -pi -e 's/\bcom\.pieq\.([^."\]]+)\.\1\b/com.pieq.$1/g'
ok "Groups normalized"

# ===================== Move packages on disk =====================
step "Moving source packages on disk: com/pieq -> com/pieq/$PKG_SEGMENT"
CURR_PATH="${CUR_BASE_PKG//./\/}"         # com/pieq
NEW_PATH="${CURR_PATH}/${PKG_SEGMENT}"    # com/pieq/<segment>
moved_dirs=0
while IFS= read -r SRC; do
  if [[ -d "$SRC/$CURR_PATH" ]]; then
    log "rsync $SRC/$CURR_PATH -> $SRC/$NEW_PATH"
    mkdir -p "$SRC/$NEW_PATH"
    rsync -a "$SRC/$CURR_PATH/" "$SRC/$NEW_PATH/"
    rm -rf "$SRC/$CURR_PATH"
    ((moved_dirs++))
  fi
done < <(find . -type d \( -path "*/src/main/kotlin" -o -path "*/src/test/kotlin" \))
ok "Moved in $moved_dirs source roots"

# ===================== Guarded replacements (skip com.pieq.core) =====================
step "Updating package/import statements (guarded; preserving com.pieq.core)"
files_touched=0
while IFS= read -r -d '' f; do
  perl -0777 -pi -e '
    s/\b\Q'"$CUR_BASE_PKG"'\E(?!\.(?:'"$PKG_SEGMENT"'|core))\b/'"$NEW_GROUP"'/g;
    s/\b\Q'"$CUR_BASE_PKG"'\E\.(?!'"$PKG_SEGMENT"'\.|core\.)/'"$NEW_GROUP"'./g
  ' "$f" && ((files_touched++)) || true
done < <(find . -type f \( -name "*.kt" -o -name "*.kts" -o -name "*.java" \) -print0)
ok "Updated $files_touched files"

# ===================== Locate & rename Application/Configuration files =====================
step "Renaming Application/Configuration files + classes"
APP_SEARCH_ROOT="$NEW_APP/src/main/kotlin"
CFG_SEARCH_ROOT="$NEW_APP/src/main/kotlin"

APP_OLD_FILE="$(find "$APP_SEARCH_ROOT" -type f -name "${CUR_APP_CLASS}.kt" -print -quit || true)"
if [[ -n "${APP_OLD_FILE:-}" && -f "$APP_OLD_FILE" ]]; then
  app_dir="$(dirname "$APP_OLD_FILE")"
  log "Application file: $(basename "$APP_OLD_FILE") -> ${CLASS_PREFIX}Application.kt"
  mv "$APP_OLD_FILE" "$app_dir/${CLASS_PREFIX}Application.kt"
else
  warn "No ${CUR_APP_CLASS}.kt found under $APP_SEARCH_ROOT"
fi

CFG_OLD_FILE="$(find "$CFG_SEARCH_ROOT" -type f -name "${CUR_CFG_CLASS}.kt" -print -quit || true)"
if [[ -n "${CFG_OLD_FILE:-}" && -f "$CFG_OLD_FILE" ]]; then
  cfg_dir="$(dirname "$CFG_OLD_FILE")"
  log "Configuration file: $(basename "$CFG_OLD_FILE") -> ${CLASS_PREFIX}Configuration.kt"
  mv "$CFG_OLD_FILE" "$cfg_dir/${CLASS_PREFIX}Configuration.kt"
else
  warn "No ${CUR_CFG_CLASS}.kt found under $CFG_SEARCH_ROOT"
fi

# in-file class renames
find . -type f -name "*.kt" -print0 | xargs -0 perl -pi -e \
  "s/\\b${CUR_APP_CLASS}\\b/${CLASS_PREFIX}Application/g; s/\\b${CUR_CFG_CLASS}\\b/${CLASS_PREFIX}Configuration/g"
ok "Application/Configuration classes renamed in files"

# ===================== Rename feature classes (Resource/Service/DAO/Client/Model) =====================
step "Renaming Resource/Service/DAO/Client/Model files + references"
APP_KT_ROOT="$NEW_APP/src/main/kotlin"
CLIENT_KT_ROOT="$NEW_CLIENT/src/main/kotlin"
API_MODEL_ROOT="$NEW_API/src/main/kotlin"

for pair in \
  "$APP_KT_ROOT:${CUR_RESOURCE_CLASS}:${CUR_MODEL_CLASS}Resource" \
  "$APP_KT_ROOT:${CUR_SERVICE_CLASS}:${CUR_MODEL_CLASS}Service" \
  "$APP_KT_ROOT:${CUR_DAO_CLASS}:${CUR_MODEL_CLASS}Dao" \
  "$CLIENT_KT_ROOT:${CUR_CLIENT_CLASS}:${CUR_MODEL_CLASS}Client" \
  "$API_MODEL_ROOT:${CUR_MODEL_CLASS}:${CUR_MODEL_CLASS}"
 do
  root="${pair%%:*}"; rest="${pair#*:}"
  from="${rest%%:*}"; to="${rest##*:}"
  old="$(find "$root" -type f -name "${from}.kt" -print -quit || true)"
  if [[ -n "${old:-}" && -f "$old" ]]; then
    d="$(dirname "$old")"
    log "Rename: $(basename "$old") -> ${to}.kt"
    mv "$old" "$d/${to}.kt"
  else
    warn "Not found: ${from}.kt under ${root} (ok if template changed)"
  fi
 done

# update references to new names
find . -type f -name "*.kt" -print0 | xargs -0 perl -pi -e \
  "s/\\b${CUR_RESOURCE_CLASS}\\b/${CUR_MODEL_CLASS}Resource/g; s/\\b${CUR_SERVICE_CLASS}\\b/${CUR_MODEL_CLASS}Service/g; s/\\b${CUR_DAO_CLASS}\\b/${CUR_MODEL_CLASS}Dao/g; s/\\b${CUR_CLIENT_CLASS}\\b/${CUR_MODEL_CLASS}Client/g; s/\\b${CUR_MODEL_CLASS}\\b/${CUR_MODEL_CLASS}/g"
ok "References updated"

# ===================== Align files to package path =====================
step "Aligning Kotlin files to match their package path"
moved_files=0
while IFS= read -r kt; do
  case "$kt" in
    */src/main/kotlin/*) SRCROOT="${kt%%/src/main/kotlin/*}/src/main/kotlin" ;;
    */src/test/kotlin/*) SRCROOT="${kt%%/src/test/kotlin/*}/src/test/kotlin" ;;
    *) continue ;;
  esac
  PKG_LINE=$(grep -m1 '^package ' "$kt" || true)
  [[ -z "$PKG_LINE" ]] && continue
  PKG=$(echo "$PKG_LINE" | sed -E 's/^package[[:space:]]+([^[:space:]]+).*$/\1/')
  [[ -z "$PKG" ]] && continue
  DEST_DIR="$SRCROOT/$(echo "$PKG" | tr '.' '/')"
  [[ -d "$DEST_DIR" ]] || { log "mkdir -p $DEST_DIR"; mkdir -p "$DEST_DIR"; }
  CUR_DIR="$(dirname "$kt")"
  if [[ "$CUR_DIR" != "$DEST_DIR" ]]; then
    log "mv $(basename "$kt") -> $DEST_DIR"
    mv "$kt" "$DEST_DIR/"
    ((moved_files++))
  fi
done < <(find . -type f -name "*.kt")
ok "Moved $moved_files Kotlin files to package-correct folders"

# Remove empty dirs ONLY under src/**/kotlin
step "Removing empty package directories"
find . \( -path "*/src/main/kotlin/*" -o -path "*/src/test/kotlin/*" \) -type d -empty -print -delete | sed 's/^/[deleted] /' || true
ok "Empty dirs cleaned"

# ===================== Update Gradle mainClass/manifest to actual ApplicationKt =====================
step "Updating Gradle mainClass to actual ApplicationKt"
APP_BUILD="$NEW_APP/build.gradle.kts"
if [[ -f "$APP_BUILD" ]]; then
  APP_NEW_FILE="$(find "$NEW_APP/src/main/kotlin" -type f -name "${CLASS_PREFIX}Application.kt" -print -quit || true)"
  if [[ -n "${APP_NEW_FILE:-}" && -f "$APP_NEW_FILE" ]]; then
    REL="${APP_NEW_FILE#${NEW_APP}/src/main/kotlin/}"
    PKG_DIR="$(dirname "$REL")"
    PKG_FQCN="$(echo "$PKG_DIR" | tr '/' '.')"
    NEW_APP_KT_FQCN="${PKG_FQCN}.${CLASS_PREFIX}ApplicationKt"
    log "mainClass.set(\"$NEW_APP_KT_FQCN\")"
    perl -pi -e "s#\"com\.pieq\.[^\"]*\.PieqApiApplicationKt\"#\"$NEW_APP_KT_FQCN\"#g" "$APP_BUILD"
    perl -pi -e "s#(Main-Class\"\\] = )\"com\.pieq\.[^\"]*\.PieqApiApplicationKt\"#\${1}\"$NEW_APP_KT_FQCN\"#g" "$APP_BUILD"
    ok "Gradle mainClass/manifest updated"
  else
    warn "Could not locate ${CLASS_PREFIX}Application.kt to compute FQCN"
  fi
else
  warn "$APP_BUILD not found"
fi

# ===================== YAML updates (module names, packages, name:) =====================
step "Updating YAML files"
yaml_changed=0
while IFS= read -r -d '' f; do
  log "YAML: $f"
  perl -pi -e "s/\\b${CUR_SUB_API}\\b/${NEW_API}/g; s/\\b${CUR_SUB_APP}\\b/${NEW_APP}/g; s/\\b${CUR_SUB_CLIENT}\\b/${NEW_CLIENT}/g" "$f"
  perl -0777 -pi -e '
    s/\b\Q'"$CUR_BASE_PKG"'\E(?!\.(?:'"$PKG_SEGMENT"'|core))\b/'"$NEW_GROUP"'/g;
    s/\b\Q'"$CUR_BASE_PKG"'\E\.(?!'"$PKG_SEGMENT"'\.|core\.)/'"$NEW_GROUP"'./g
  ' "$f"
  sed -E -i '' "s/^( *name *: *).*/\1\"${SERVICE_NAME}\"/g" "$f" || true
  ((yaml_changed++))
done < <(find . -type f \( -name "*.yml" -o -name "*.yaml" \) -print0)
ok "YAML files updated: $yaml_changed"

# ===================== Dockerfile updates (module names, jar references) =====================
step "Updating Dockerfile(s)"
for DOCKER in Dockerfile docker.local; do
  if [[ -f "$DOCKER" ]]; then
    log "Patching $DOCKER for module renames"
    # Replace module directory references
    perl -pi -e "s/\\b${CUR_SUB_API}\\b/${NEW_API}/g; s/\\b${CUR_SUB_APP}\\b/${NEW_APP}/g; s/\\b${CUR_SUB_CLIENT}\\b/${NEW_CLIENT}/g" "$DOCKER"

    # Normalize any hardcoded versioned jar names like <module>-1.0.jar -> <module>.jar
    perl -pi -e "s/(${NEW_APP})-([0-9]+(?:\.[0-9]+)*)\.jar/\1.jar/g" "$DOCKER" || true

    ok "$DOCKER updated"
  else
    warn "$DOCKER not found (skipped)"
  fi
done

# ===================== Summary =====================
step "Summary"
log "Repo root      : $(pwd)"
log "Modules        : $(ls -1d ${SERVICE_NAME}-* 2>/dev/null | tr '\n' ' ')"
log "Gradle groups  :"
grep -R "^[[:space:]]*group[[:space:]]*=" -n . || true
log "Inter-project deps:"
grep -R "project(" -n . || true

ok "Bootstrap complete 🎉"
echo
echo "Build:"
echo "  ./gradlew clean build"
echo
echo "Run:"
echo "  (cd ${NEW_APP} && ./gradlew installDist && ./build/install/${NEW_APP}/bin/${NEW_APP} server src/main/resources/config_dev.yml)"
