#!/bin/bash

file=$1
nameCertificat=$2
Entitlements=$3

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

if [ -z "$file" ]; then
  echo "❌ Provide a file path $file"
	exit 1
fi

# fill with default values
if [ -z "$nameCertificat" ]; then
	nameCertificat="Developer ID Application"
fi

if [ -z "$Entitlements" ]; then
	Entitlements="SDK.entitlements"
fi

signApp="$SCRIPT_DIR/SignApp.sh"

workTmp=$(mktemp -d)

cd "$SCRIPT_DIR"
has_bin=$(unzip -l "$file" | grep "jnilib\|dylib" | wc -l) #\|so
if [[ $has_bin -gt 0 ]]; then
  echo "📦 $file"
  name=$(basename $file)
  workPath="$workTmp/$name"
  mkdir -p "$workPath"

  if [[ "$OSTYPE" == "darwin"* ]]; then
    ditto -x -k "$file" "$workPath" # ditto have less issue with file name encoding
  else
    unzip -qq "$file" -d "$workPath"
  fi
  if [ "$?" -gt 0 ]; then
    echo "❌ Failed to unzip $file"
    exit 1
  fi
  "$signApp" "$nameCertificat" "$workPath" "$Entitlements"
  if [ "$?" -gt 0 ]; then
    echo "❌ Failed to sign $file"
    exit 1
  fi
  rm "$file"
  cd "$workPath"; zip -qq -r "$file" .
  retVal=$?
  if [ "$retVal" -gt 0 ]; then
    echo "❌ ($retVal) Failed to archive $file (maybe jar lost)"
    exit 1
  else
    echo "✅ signed and zip to $file"
  fi
fi
