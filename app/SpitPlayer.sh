#!/bin/bash

exec ./java/bin/java -cp .:SpitPlayer.jar:lib/* -Xms50m -Xmx4g -XX:MinHeapFreeRatio=5 -XX:MaxHeapFreeRatio=10 -XX:+UseStringDeduplication -XX:+UseCompressedOops -Dfile.encoding=UTF-8 --illegal-access=permit sp.it.pl.main.AppKt "$@"