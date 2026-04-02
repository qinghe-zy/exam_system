$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..\\backend')
mvn -q -DskipTests package
java -jar target/exam-system-backend-0.1.0-SNAPSHOT.jar
