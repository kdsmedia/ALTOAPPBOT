#!/bin/bash

binPath=$(which brew)
if [ -z "$binPath" ]; then
    /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
else
    echo "✅ $binPath"
fi

binPath=$(which kscript)
if [ -z "$binPath" ]; then
    brew install holgerbrandl/tap/kscript
else
    echo "✅ $binPath"
fi

binPath=$(which gradle)
if [ -z "$binPath" ]; then
    brew install gradle
else
    echo "✅ $binPath"
fi
