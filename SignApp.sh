#!/bin/bash


#*******************************************************************
#Script permettant la signature d'une application
# Parametres :
# 1 : Nom du certificat
# 2 : Chemin de l'application
#
#Retour 0 quand tous c'est bien passé
#ou Autre quand il y a une erreur
#*******************************************************************

#*******************************************************************
# function permettant de signer un fichier
# Parametres :
# 1 : Flag permettant de retourner l'erreur
# 2 : Nom du certificat
# 3 : Chemin du fichier
# 4 : Chemin du fichier 4D.entitlements (for .app only)
#
# Retour : Renvoi du flag d'erreur
#*******************************************************************
#set -x 
files=()

nameCertificat=$1
PathApp=$2
Entitlements=$3
LogPath=$4
boolError=0

filesToSign=('dylib' 'jnilib')
foldersToCheck=('Contents/Library/LaunchServices')
commands=""
IFS=$'
'

function PrepareCommand ()
{
    commands="find '$1'"

    for (( idx=0; idx<${#filesToSign[@]} ; idx++ )) ; do
        if [ $idx -ne 0 ]; then
            commands+=' -o'
        fi
        commands+=" -name '*.${filesToSign[idx]}'"
    done
}

function SignFile ()
{
    CertifName="$1"
    Entitlements="$2"
    file="$3"
    echo "$3"
    
    if [ ! -L "$file" ]
        then
        if codesign -dvv --deep "$3" 2>&1 | grep -qF "Signature=adhoc"
        then
            echo codesign -f --sign "$CertifName" --verbose --timestamp --options runtime --entitlements "$Entitlements" "$3"
            codesign -f --sign "$CertifName" --verbose --timestamp --options runtime --entitlements "$Entitlements" "$3" > "sign_output.txt" 2>&1
        else
            echo --sign "$CertifName" --verbose --timestamp --options runtime --entitlements "$Entitlements" "$3"
            codesign --sign "$CertifName" --verbose --timestamp --options runtime --entitlements "$Entitlements" "$3" > "sign_output.txt" 2>&1
        fi
        result=$?
        if [ ${result} -eq 0 ] || grep -q "is already signed" sign_output.txt; then
            echo "Buildmgr message:  [codesign]  SUCCESS  ${3}"
        else
            echo "Buildmgr message:  [codesign]  ERROR  ${3}" >&2
            cat sign_output.txt >&2
        fi
    fi
    return ${result}
}

function Sign ()
{

    FlagError="$1"
    CertifName="$2"
    PathSign="$3"
    Entitlements="$4"
    logSign="$5"
 
    if [[ ! -z $logSign ]]; then
        exec > $logSign
    fi

    PrepareCommand "$PathSign"
    xattr -rc "$PathSign"

    echo "commands $commands"
    for line in $(eval $commands)
    do
        files+=("$line")
    done
    

    #Convert the name of "info.plist" to "Info.plist"
    for line in $(eval "find '$PathSign' -path '*/*.bundle/*' -name info.plist")
    do
        mv $line $(dirname "$line")/Info.plist
    done

    #Sign in revert order
    for (( idx=${#files[@]}-1 ; idx>=0 ; idx-- )) ; do

        for (( folderI=0; folderI<${#foldersToCheck[@]} ; folderI++ )) ; do
            local fullPath="${files[idx]}"/"${foldersToCheck[folderI]}"
            if test -d "$fullPath" ; then
            for line in "$fullPath"/*
            do
                if [ -f $line ]; then
                    SignFile "$CertifName" "$Entitlements" $line
                    FlagError=$?
                fi
            done
            fi
        done
         
        SignFile "$CertifName" "$Entitlements" "${files[idx]}"

    done

    #The last one is the app folder.
    return $FlagError

}
if [ -d ${PathApp} ]
then
	Sign $boolError "${nameCertificat}" "${PathApp}" "${Entitlements}" "${LogPath}"
else
	SignFile "${nameCertificat}" "${Entitlements}" "${PathApp}"
fi
boolError=$?
