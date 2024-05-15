# Stickers by tag Telegram bot
Inline telegram bot that finds stickers by user-defined tags!
Associate your stickers with any character string (no whitespaces) trough private messages with the bot, 
then use it inline to quickly find those stickers when you need them.

I wrote this bot to practice my Java skills. It uses MySQL database to store user info and XML file for configuration.

## Dependencies
`stickerByTagTGBot` depends on:
* Java 11 (java.net.http.HttpClient)
* [gson-2.8.9](https://github.com/google/gson)
* [my-sql-connector-java](https://dev.mysql.com/downloads/connector/j/)

## Build and run
Assuming you have source code and all dependencies in the same directory, you can build and run with:
```
javac -cp "*:" *.java
java -cp "*:" Bot
```
Replace ':' with ';' on Windows!
