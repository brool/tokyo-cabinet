#!/bin/bash

wget http://1978th.net/tokyocabinet/javapkg/tokyocabinet-java-1.22.tar.gz

tar zxf tokyocabinet-java-1.22.tar.gz

cd tokyocabinet-java-1.22

./configure

make && sudo make install

cp *.jar ../lib/

cd -


rm -rf tokyocabinet-java*

