rm -rf ./submission

mkdir ./submission
mkdir ./submission/sql
mkdir ./submission/src

cp -r ./sql/* ./submission/sql/
cp -r ./src/* ./submission/src/
cp design.pdf ./submission/design.pdf
cp ReadMe.txt ./submission/ReadMe.txt
cp run.sh ./submission/run.sh

tar -cvf submission.tar ./submission

# turnin cs460p4 submission.tar