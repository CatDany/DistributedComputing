@echo off
echo Running as CLIENT
set /p BFip="Server address (empty for localhost): "
set /p BFport="Server port: "
IF [%BFip%] EQU [] (
  set BFip=localhost
  )
java -jar "bfdist_1.0-a4.jar" client %BFport% %BFip% --enableDebugLogging
pause