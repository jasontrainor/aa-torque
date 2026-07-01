#!/bin/bash
set -e

# Default increment is patch
INC_TYPE="patch"

# Parse arguments
while [[ "$#" -gt 0 ]]; do
    case $1 in
        --major) INC_TYPE="major" ;;
        --minor) INC_TYPE="minor" ;;
        --patch) INC_TYPE="patch" ;;
        *) echo "Unknown parameter passed: $1"; exit 1 ;;
    esac
    shift
done

# Read properties (handling potential \r from Windows)
MAJOR=$(grep -oP '^majorVersion=\K[0-9]+' version.properties)
MINOR=$(grep -oP '^minorVersion=\K[0-9]+' version.properties)
PATCH=$(grep -oP '^patchVersion=\K[0-9]+' version.properties)

if [ "$INC_TYPE" == "major" ]; then
    MAJOR=$((MAJOR + 1))
    MINOR=0
    PATCH=0
elif [ "$INC_TYPE" == "minor" ]; then
    MINOR=$((MINOR + 1))
    PATCH=0
else
    PATCH=$((PATCH + 1))
fi

# Write back
cat <<EOF > version.properties
majorVersion=$MAJOR
minorVersion=$MINOR
patchVersion=$PATCH
buildNumber=
EOF

VERSION_STRING="${MAJOR}.${MINOR}.${PATCH}"
echo "Building Release APK for version $VERSION_STRING..."

./gradlew assembleRelease

echo "Copying APK to project root..."
# Find the generated release apk
APK_FILE=$(find app/build/outputs/apk/release -name "*.apk" | head -n 1)

if [ -n "$APK_FILE" ]; then
    cp "$APK_FILE" "./aa-torque-v${VERSION_STRING}.apk"
    echo "Successfully created ./aa-torque-v${VERSION_STRING}.apk"
else
    echo "Error: Could not find the generated APK."
    exit 1
fi
