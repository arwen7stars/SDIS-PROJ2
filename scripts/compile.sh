#!/bin/bash

rm -r bin
mkdir -p bin

javac $(find ../ -name "*.java") -d bin
