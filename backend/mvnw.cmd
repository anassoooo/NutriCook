@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements. See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership. The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License. You may obtain a copy of the License at
@REM
@REM https://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied. See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@IF "%__MVNW_ARG0_NAME__%"=="" (SET "BASE_DIR=%~dp0") ELSE (SET "BASE_DIR=%__MVNW_ARG0_NAME__%")
@SET "BASE_DIR=%BASE_DIR:~0,-1%"

@IF NOT "%MVNW_REPOURL%" == "" (
  SET "MVNW_REPOURL=%MVNW_REPOURL%"
)

@SET WRAPPER_DIR=%BASE_DIR%\.mvn\wrapper
@SET WRAPPER_JAR=%WRAPPER_DIR%\maven-wrapper.jar
@SET WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain
@SET WRAPPER_URL=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar

@SET DOWNLOAD_URL=%WRAPPER_URL%

@IF EXIST "%WRAPPER_JAR%" (
    @REM Wrapper jar already present, skip download
) ELSE (
    @IF NOT "%MVNW_REPOURL%" == "" (
        SET DOWNLOAD_URL="%MVNW_REPOURL%/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"
    )
    @echo Downloading Maven Wrapper...
    @powershell -Command "& {Invoke-WebRequest -Uri '%DOWNLOAD_URL%' -OutFile '%WRAPPER_JAR%'}"
)

@SET JAVA_HOME_LOCAL=%JAVA_HOME%
@IF "%JAVA_HOME_LOCAL%"=="" (
    @FOR /f "delims=" %%i IN ('where java 2^>nul') DO (
        @SET "JAVA_BIN=%%i"
        @GOTO :found_java
    )
    :found_java
    @IF "%JAVA_BIN%"=="" (
        @echo Error: JAVA_HOME not set and java not found on PATH.
        @EXIT /B 1
    )
    @FOR %%i IN ("%JAVA_BIN%") DO SET "JAVA_BIN_DIR=%%~dpi"
    @SET "JAVA_HOME_LOCAL=%JAVA_BIN_DIR%.."
)

@SET JAVA_EXE=%JAVA_HOME_LOCAL%\bin\java.exe
@IF NOT EXIST "%JAVA_EXE%" SET "JAVA_EXE=java"

"%JAVA_EXE%" -classpath "%WRAPPER_JAR%" "%WRAPPER_LAUNCHER%" %MAVEN_OPTS% %MAVEN_CONFIG% %*
