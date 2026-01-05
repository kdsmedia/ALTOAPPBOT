#!/bin/bash

componentPath="$1"
projectFile="$2"
cmd=$(basename $0)

# if we pass only 4dmobile and not component path we use it
if [ -z "$projectFile" ]; then
   if [[ $componentPath == *".4dmobile" || $componentPath == *".4dmobileapp"  ]]; then
     projectFile=$componentPath
     componentPath=""
   fi
fi

export JAVA_HOME=`/usr/libexec/java_home -v 1.8`
if [ -z "$JAVA_HOME" ]; then
    # use android studio by default
    export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
fi

# if no project file, use the cached one
if [ -z "$projectFile" ]; then
   projectFile=$HOME/Library/Caches/com.4d.mobile/lastBuild.android.4dmobile
else
  if [[ "$projectFile" == *".4dmobileapp" ]]; then
     echo "Converting to 4dmobile file: $projectFile..."
     resultFile="$(dirname $projectFile)/$(basename "$projectFile" .4dmobileapp).4dmobile" # CLEAN maybe use a temp dir and remote it after working on it
     jq '."project"=. | {project}' "$projectFile" > "$resultFile"
     projectFile="$resultFile"
     echo "Converted to $projectFile"
  fi
fi

function usage() {
    >&2 echo "usage $cmd (<component path>) (<.4dmobile>) "
    >&2 echo "  component path: the root path of your component"
    >&2 echo "  .4dmobile: the project info file (by default $HOME/Library/Caches/com.4d.mobile/lastBuild.android.4dmobile)"
    >&2 echo "Please define also: "
    >&2 echo "  PERFORCE_PATH env var to the root of your perforce (we will find component inside)"
    >&2 echo "  or MOBILE_COMPONENT_PATH env var to the root the c4d mobile component"
    >&2 echo "  or if your 4D is in /Applications/ we will use it by defaults"
    >&2 echo "❌ failure"
    exit 42
}

# Get current script directory and move in it
SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do # resolve $SOURCE until the file is no longer a symlink
  DIR="$( cd -P "$( dirname "$SOURCE" )" >/dev/null 2>&1 && pwd )"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE" # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
done
DIR="$( cd -P "$( dirname "$SOURCE" )" >/dev/null 2>&1 && pwd )"

cd "$DIR"

if [ -z "$MOBILE_COMPONENT_PATH" ]; then
  mobileComponentPath="/Applications/4D.app/Contents/Resources/Internal User Components/4D Mobile App.4dbase"
  if [ ! -z "$PERFORCE_PATH" ]; then
      branch="main"
      mobileComponentPath="$PERFORCE_PATH/4eDimension/$branch/4DComponents/Internal User Components/4D Mobile App" 
  fi
else
  mobileComponentPath="$MOBILE_COMPONENT_PATH"
fi

if [ ! -d "$mobileComponentPath" ]; then
    >&2 echo "To copy template we need mobile component but '$mobileComponentPath' do not exists"
  usage
fi

# if component path not defined use composant base
if [ -z "$componentPath" ]; then
    componentPath="$mobileComponentPath" 
fi

# launch compilation and bundling
kscript main.kt generate \
    --project-editor "$projectFile" \
    --files-to-copy  "$mobileComponentPath/Resources/templates/android/project/copy/" \
    --template-files "$mobileComponentPath/Resources/templates/android/project/tpl/" \
    --template-forms "$mobileComponentPath/Resources/templates/form/" \
    --host-db        "$componentPath/Resources/mobile/" 

status=$?
if [ "$status" -eq 0 ]; then
    echo "✅ success"
else
    >&2 echo "❌ failure"
    exit $status
fi
