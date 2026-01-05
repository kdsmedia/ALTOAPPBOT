#!/bin/sh

source "$HOME/.sdkman/bin/sdkman-init.sh"

has=$(which sdk)
status=$? # seem to not working for sdk in script

if [ "$status" -ne "0" ]; then
  curl -s "https://get.sdkman.io" | bash
  source "$HOME/.sdkman/bin/sdkman-init.sh"
fi

sdk version

has=$(which kscript)
if [ "$?" -ne "0" ]; then
  sdk install kscript 4.2.2
fi

version=$(kscript  --version 2>&1 | grep "Version" | sed 's/.*: //')
if [[ "$version" = "4"* ]]
then
	echo "✅ kscript $version"
else
	echo "❌ kscript $version. Need v4"
  sdk install kscript 4.2.2
  sdk use kscript 4.2.2
fi

java=$(sdk list java | grep 17.0 | grep "open" | sed 's/.*| //')
echo "java $java"
sdk install java $java
sdk use java $java


version=$(gradle -v 2>&1 | grep "Gradle " | sed 's/.* //')
if [[ "$version" = "8."* ]]
then
	echo "✅ gradle $version"
else
	echo "❌ gradle $version. Need v8"
  sdk install gradle 8.1.1
  sdk use gradle 8.1.1
fi

echo "if sdk installed do not forget to do source $HOME/.sdkman/bin/sdkman-init.sh"