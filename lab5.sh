#!/bin/bash

mkdir out 2> /dev/null

cd out

jar xf ../artifacts/info.kgeorgiy.java.advanced.implementor.jar \
        info/kgeorgiy/java/advanced/implementor/Impler.class \
        info/kgeorgiy/java/advanced/implementor/JarImpler.class \
        info/kgeorgiy/java/advanced/implementor/ImplerException.class

mkdir ../jar-files 2> /dev/null

jar cfm ../jar-files/Implementor.jar ../java/com/botsula/labs/implementor/Manifest.txt \
     com/botsula/labs/implementor/*.class \
     info/kgeorgiy/java/advanced/implementor/*.class