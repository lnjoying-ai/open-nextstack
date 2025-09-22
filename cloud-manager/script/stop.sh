#!/bin/bash

for pid in `ps -aef | grep 'com.justice\|com.lnjoying' | grep -v grep | awk '{print $2;}'`
do
    kill -9 $pid
done
