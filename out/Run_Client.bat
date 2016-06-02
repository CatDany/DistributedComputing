@echo off
echo Running as CLIENT
set /p BFip="Server address (empty for localhost): "
set /p BFport="Server port: "
IF [%BFip%] EQU [] (
  set BFip=localhost
  )
java -jar "bfdist_2.1-a5.jar" client %BFport% %BFip%
pause