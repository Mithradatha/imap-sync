Execution Instructions
===

1. `cd ImapProject`
2. `javac -classpath ".;.\libraries\commons-cli-1.3.1\commons-cli-1.3.1.jar" .\src\*.java`
3. `java -classpath ".;.\libraries\commons-cli-1.3.1\commons-cli-1.3.1.jar;.\src" -Djavax.net.debug=ssl:handshake Client -S imap.gmail.com -P 993 -l <email_address> -p <password>`
