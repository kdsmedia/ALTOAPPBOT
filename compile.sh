#!/bin/bash

dest="$1"
cmd=$(basename $0)

function usage() {
    >&2 echo "Please define: "
    >&2 echo "  PERFORCE_PATH env var to the root of your perforce to submit to perforce later"
    >&2 echo "  or PJGEN_PATH env var, ex: export PJGEN_PATH=\"/Applications/4D.app/Contents/Resources/Internal User Components/4D Mobile App.4dbase/Resources/scripts/\""
    >&2 echo "  or "$cmd" <destination folder>"
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

# launch compilation and bundling
kscript --package main.kt
mv main androidprojectgenerator.jar

# move it
if [ ! -z "$dest" ]
then
    echo "[compile] copy to $dest"
    mv -f androidprojectgenerator.jar "$dest"
else

    if [ -z "$PJGEN_PATH" ]
    then
        if [ -z "$PERFORCE_PATH" ]
        then
            relativePerforcePath="../../4DComponents/Internal User Components/4D Mobile App/Resources/scripts/"
            if [ -d "$relativePerforcePath" ]; then
                echo "[compile] no PERFORCE_PATH env var defined, try to copy to relative directory"
                mv -f androidprojectgenerator.jar "../../4DComponents/Internal User Components/4D Mobile App/Resources/scripts/"
            else
                >&2 echo "[compile] no destination defined. You will find 'androidprojectgenerator.jar' in this directory (do not commit it)"
                usage
            fi
        else
            echo "[compile] copy to $PERFORCE_PATH"
            mv -f androidprojectgenerator.jar "$PERFORCE_PATH/4eDimension/main/4DComponents/Internal User Components/4D Mobile App/Resources/scripts/"
        fi
    else
        echo "[compile] copy to $PJGEN_PATH"
        mv -f androidprojectgenerator.jar "$PJGEN_PATH/"
    fi
fi

status=$?
if [ "$status" -eq 0 ]; then
    echo "✅ success"
else
    >&2 echo "❌ failure"
fi
