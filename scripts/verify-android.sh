#!/usr/bin/env bash
set -euo pipefail
project_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$project_dir/.local-tools/gradle-home}"
export ANDROID_USER_HOME="${ANDROID_USER_HOME:-$project_dir/.local-tools/android-user}"
cd "$project_dir"
if (( $# )); then
    exec ./gradlew --console=plain "$@"
fi
exec ./gradlew --console=plain :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:ktlintCheck :app:assembleDebugAndroidTest
