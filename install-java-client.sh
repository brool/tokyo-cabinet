#!/bin/bash

wget http://downloads.sourceforge.net/project/tokyocabinet/tokyocabinet-java/1.22/tokyocabinet-java-1.22.tar.gz

tar zxf tokyocabinet-java-1.22.tar.gz

cd tokyocabinet-java-1.22

./configure

make && sudo make install

cp *.jar ../lib/

cd -


rm -rf tokyocabinet-java*

