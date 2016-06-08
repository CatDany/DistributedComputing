@echo off
echo Running as SERVER
set /p BFport="Server port: "
java -jar "bfdist_2.2-a8.jar" server %BFport%
pause