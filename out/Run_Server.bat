@echo off
echo Running as SERVER
set /p BFport="Server port: "
java -jar "bfdist_2.1-a7.jar" server %BFport%
pause