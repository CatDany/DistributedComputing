@echo off
echo Running as SERVER
set /p BFport="Server port: "
java -jar "bfdist_2.0-a3.jar" server %BFport%
pause