#!/bin/sh

lein uberjar
java -jar target/jrpg-0.1.0-SNAPSHOT-standalone.jar
