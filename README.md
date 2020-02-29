Execution Instructions
===
Compile: `javac -classpath ".;.\libraries\commons-cli-1.3.1\commons-cli-1.3.1.jar" .\src\*.java`

Run: `java -classpath ".;.\libraries\commons-cli-1.3.1\commons-cli-1.3.1.jar;.\src" -Djavax.net.debug=ssl:handshake Client -S imap.gmail.com -P 993 -l <email_address> -p <password>`
