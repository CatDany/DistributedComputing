### What is distributed computing?
Well, google it. Here's an [article on the Wikipedia](https://en.wikipedia.org/wiki/Distributed_computing).

TL;DR: Some calculations take a really long time, so when you have multiple computers that work on the same thing simultaneously,
where each one calculates its own part of the whole task, the work is going to be done faster.
This is exactly what this code is supposed to do. But because it's a school project I don't intend to make it flexible (maybe API in the future)

### How do I run it?
Get the code and compile it or [go to my Dropbox](https://drive.google.com/folderview?id=0B1QFErsUwxi9R3F4ZUpOYXo3Z0k&usp=sharing) to get a compiled jar. Then run with the following arguments:
```
Pattern: (client|server) (port) [server-ip] [--enableDebugLogging, optional]
Client example: client 12345 127.0.0.1 --enableDebugLogging
Server example: server 12345 --enableDebugLogging
```
There are also batch files in the '[out](https://github.com/CatDany/DistributedComputing/tree/master/out)' folder that you can use.

### Server, client.. huh?
So **server-side** is the director here. It takes a certain task from the user and tells each of its clients what part of the task they're going to be doing.

Where as **client-side** is the calculator machine itself.

### More info
I didn't want this readme to be really long, but if you want to ask me some questions about the project, you can either PM me on GitHub or send a tweet to [@CatDany](https://twitter.com/CatDanyRU).
