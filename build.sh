#!/bin/bash

BINARY_NAME=androidprojectgenerator.jar
BINARY_PATH="$(pwd)/$BINARY_NAME"
nameCertificat="Developer ID Application"

if [ -z "$JAVA_HOME" ]; then
  if [ -d "/Applications/Android Studio.app/Contents/jbr/Contents/Home" ]; then
    export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
  fi
fi

has=$(which java)
if [ "$?" -ne "0" ]; then
  echo "❌ no java, install it"
  exit 2
fi
version=$(java -version)
echo "ℹ️ java 11 or 17 required"
# TODO: check java version for installed kscript?


has=$(which kscript)
if [ "$?" -ne "0" ]; then
  echo "no kscript in path, install it"
  # brew install kscripting/tap/kscript
  echo "with sdkman: sdk install kscript 4.2.2"
  echo "or maybe do $HOME/.sdkman/bin/sdkman-init.sh"
  exit 1
fi

version=$(kscript  --version 2>&1 | grep "Version" | sed 's/.*: //')
if [[ "$version" = "4"* ]]
then
	echo "✅ kscript $version"
else
	echo "❌ kscript $version. Need v4"
  echo "with sdkman: sdk install kscript 4.2.2"
  echo "and maybe: sdk use kscript 4.2.2"
  
  exit 1
fi

has=$(which kotlin)
if [ "$?" -ne "0" ]; then
  echo "❌ no kotlin, install it."
  echo "with sdkman: sdk install kotlin"
  exit 1
fi
version=$(kotlin -version)
echo "$version"
has=$(which sdk)
if [ "$?" -eq "0" ]; then
  sdk current kotlin # check also sdk version in case of
fi
echo "ℹ️ kotlin need 1.7 version mininum (if use sdk for other tools, check)"
# TODO: check kotlin version for installed kscript?

version=$(gradle -v 2>&1 | grep "Gradle " | sed 's/.* //')

if [[ "$version" = "8."* ]]
then
	echo "✅ gradle $version"
else
	echo "❌ gradle $version. Need version 8"
  echo "with sdkman: sdk install gradle 8.1.1"
  echo "or brew ..."
  
  exit 1
fi

export KSCRIPT_DIRECTORY=$(pwd) # to force cache path inside current directory

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
cd "$SCRIPT_DIR"

kscript --clear-cache
kscript --package main.kt 2>&1 | sed 's/\[nl\]/\n/g'

jar_path=$(find "$KSCRIPT_DIRECTORY" -name "main.jar")

if [ -f "$jar_path" ]; then
  mv "$jar_path" "$BINARY_PATH"
  echo "🎉 binary available at path $BINARY_PATH"


  if [[ "$OSTYPE" == "darwin"* ]]; then
    security find-certificate -c "$nameCertificat" > /dev/null 2>&1
    retVal=$?
    if [ "$retVal" -eq 0 ];then
      ./sign.sh "$BINARY_PATH"
    else
      echo "ℹ️ Jar binaries are not signed. Do not found certificate for $nameCertificat"
    fi
  fi

else
  echo "❌ no binary produced"
  exit 3
fi