#!/bin/sh
  wget -qO "gradle/wrapper/gradle-wrapper.jar" "https://raw.githubusercontent.com/gradle/gradle/master/gradle/wrapper/gradle-wrapper.jar"
  java -jar gradle/wrapper/gradle-wrapper.jar wrapper
  exec "$(cd "$(dirname "$0")" && pwd)/gradlew" "$@"