@echo off
echo Running as SERVER
set /p BFport="Server port: "
java -jar "bfdist_1.0-a3.jar" server %BFport% --enableDebugLogging
pause