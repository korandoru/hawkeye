#!/usr/bin/env bash

"$JAVA_HOME"/bin/native-image \
    -cp ../../hawkeye-command/target/hawkeye-command.jar \
    io.korandoru.hawkeye.command.HawkEyeCommandMain \
    hawkeye-native
