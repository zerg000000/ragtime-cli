#!/user/bin/env bash

if [[ $OS =~ macos ]]; then
    LIBSUNEC_PATH="$JAVA_HOME/lib/libsunec.a"
else
    LIBSUNEC_PATH="$JAVA_HOME/lib/libsunec.so"
fi

cp $LIBSUNEC_PATH .
