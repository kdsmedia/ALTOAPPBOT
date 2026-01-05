#!/bin/bash

currentDir=$(pwd)
# configure needed path
export PERFORCE_PATH=$currentDir
if [ -z "$BRANCH" ]; then
    >&2 echo "❌ failure BRANCH must be deined"
    exit 1
fi
export MOBILE_COMPONENT_PATH="$PERFORCE_PATH/4eDimension/$BRANCH/4DComponents/Internal User Components/4D Mobile App"

export QMOBILE_HOME=$currentDir/sdk
export ANDROID_HOME=$HOME/Library/Android/sdk
export TARGET_PATH=$currentDir/build

rm -Rf $TARGET_PATH
mkdir $TARGET_PATH

finalStatus=0

# launch for each projet found recursively

find "$currentDir/dataForCreateProject" -name '*.4dmobileapp' | while read line; do
    echo "📦 Processing file '$line' start..."
    shortPath=$(python -c "import os.path; print os.path.relpath('$line', '$currentDir')")
    echo "##teamcity[testStarted name='$shortPath']"
	./test.sh "$line"
    status=$?
    #cat "output.log" 2>&1
    if [ $status -ne 0 ];then
    	finalStatus=$status
        # logcontent=$(cat "output.log")
        echo "##teamcity[testFailed name='$shortPath' message='see log' details='see log']"
    fi
    
    echo "##teamcity[testFinished name='$shortPath']"
    echo "📦 Processing file '$line' done"
done

exit $finalStatus