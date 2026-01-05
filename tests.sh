#!/bin/bash

# Parameters

testsPath="$1"

targetDir=$(mktemp)
if [ ! -d "$TARGET_PATH" ]; then
  export TARGET_PATH="$targetDir"
fi

if [ ! -d "$QMOBILE_HOME" ]; then
  export QMOBILE_HOME="/Library/Caches/com.4D.mobile/sdk/1920/Android/sdk"
  if [ ! -d "$QMOBILE_HOME" ]; then
    export QMOBILE_HOME="/Library/Caches/com.4D.mobile/sdk/1900/Android/sdk"
  fi
fi

if [ -z "$testsPath" ]; then
  echo "No default test path, try to look into PERFORCE_PATH or relative path to this script"
  if [ ! -z "$PERFORCE_PATH" ]; then
    testsPath="$PERFORCE_PATH/4eDimension/$branch/4D/Tests/_MobileIT/Resources/test/dataForCreateProject"
  else 
    if [ -d "../../4D/Tests/_MobileIT/Resources/test/dataForCreateProject" ]; then
       testsPath="../../4D/Tests/_MobileIT/Resources/test/dataForCreateProject"
    fi
  fi
fi

if [ ! -d "$testsPath" ]; then
  >&2 echo "You must specify a path as argument with test file or PERFORCE_PATH env var"
  exit 1
fi

# TODO
# - allow to have different output (teamcity, or less verbose with only result by file)

# Start the tests

finalStatus=0

## get python bin and version
has_python3=$(which python3)
if [ "$has_python3" == "" ]; then
  python_bin="python"
  python_version=$( $python_bin --version)
  if [[ $python_version = Python\ 3* ]]; then
     has_python3=1
  else
     has_python3=0
  fi
else
  python_bin="python3"
  has_python3=1
fi


while read line; do
    echo "📦 Processing file '$line' start..."

    if [ $has_python3 -eq 1 ]; then
      shortPath=$($python_bin -c "import os.path; print(os.path.relpath('$line', '$currentDir'))")
    else
      shortPath=$($python_bin -c "import os.path; print os.path.relpath('$line', '$currentDir')")
    fi

    # echo "##teamcity[testStarted name='$shortPath']"
	  ./test.sh "$line"
    status=$?
    if [ $status -ne 0 ];then
    	finalStatus=$status
      # echo "##teamcity[testFailed name='$shortPath' message='see log']"
    fi
    
    # echo "##teamcity[testFinished name='$shortPath']"
    echo "📦 Processing file '$line' done"
done <<EOT
$(find "$testsPath" -name '*.4dmobileapp')
EOT


echo "Final result $finalStatus"
if [ "$finalStatus" -eq 0 ]; then
    echo "✅ success"
else
    >&2 echo "❌ failure"
    exit $finalStatus
fi

rm -rf $targetDir
