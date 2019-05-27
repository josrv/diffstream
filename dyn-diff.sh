#!/bin/bash

while true
do
    diff <(cat "$1") <(sleep 1; cat "$1")
done
