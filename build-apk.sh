#!/bin/bash
set -e

echo "Building Release APK..."
./gradlew assembleRelease

echo "Copying APK to project root..."
cp app/build/outputs/apk/release/*.apk ./

echo "Build complete."
