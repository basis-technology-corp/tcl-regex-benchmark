#! /bin/sh
ver=$1
op=$2
text=$3
shift
shift
shift
mvn clean install -Ptclre-$ver
java -cp `cat classpath.txt`:target/classes com.basistech.tclrebenchmark.BenchmarkDriver $op $text "$@"

