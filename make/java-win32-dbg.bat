
set BLD_SUB=build-win32
set J2RE_HOME=c:\jre1.6.0_20_x32
set JAVA_HOME=c:\jdk1.6.0_20_x32
set ANT_PATH=C:\apache-ant-1.8.0

set PATH=%JAVA_HOME%\bin;%ANT_PATH%\bin;c:\mingw\bin;%PATH%

set BLD_DIR=..\%BLD_SUB%
set LIB_DIR=%BLD_DIR%\lib;..\..\gluegen\%BLD_SUB%\obj

set CP_ALL=.;%BLD_DIR%\jogl\jogl.all.jar;%BLD_DIR%\nativewindow\nativewindow.all.jar;%BLD_DIR%\newt\newt.all.jar;%BLD_DIR%\jogl\jogl.test.jar;..\..\gluegen\%BLD_SUB%\gluegen-rt.jar;..\..\gluegen\make\lib\junit-4.5.jar;%ANT_PATH%\lib\ant.jar;%ANT_PATH%\lib\ant-junit.jar

echo CP_ALL %CP_ALL%

%J2RE_HOME%\bin\java -classpath %CP_ALL% "-Djava.library.path=%LIB_DIR%" "-Dnativewindow.debug=all" "-Djogl.debug=all" "-Dnewt.debug=all" "-Dsun.java2d.noddraw=true" "-Dsun.awt.noerasebackground=true" %1 %2 %3 %4 %5 %6 %7 %8 %9 > java-win32-dbg.log 2>&1
