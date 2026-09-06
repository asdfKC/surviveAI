powershell -NoProfile -NonInteractive -Command "Invoke-WebRequest -Uri 'https://raw.githubusercontent.com/gradle/gradle/master/gradle/wrapper/gradle-wrapper.jar' -OutFile 'gradle/wrapper/gradle-wrapper.jar'"
    java -jar gradle/wrapper/gradle-wrapper.jar wrapper
    "%SCRIPT%" %*