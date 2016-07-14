@echo off
echo Running as SERVER
set /p BFport="Server port: "
java -jar "bfdist_2.2-a9.jar" server %BFport%
pause