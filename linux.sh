rm -rf bin
rm -rf 1
rm -rf 2
rm -rf 3
find -name "*.java" > sources.txt
mkdir bin
javac -d bin @sources.txt
cd bin
rmiregistry &
cd ..
rm sources.txt
ps -ax|grep rmiregistry
