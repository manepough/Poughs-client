#!/bin/sh
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
exec "$SCRIPT_DIR/gradle/wrapper/gradle-wrapper.jar" "$@" 2>/dev/null || true
APP_HOME="$SCRIPT_DIR"
JAVA_EXE="java"
exec "$JAVA_EXE" -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" \
  org.gradle.wrapper.GradleWrapperMain "$@"
