server : ajax-connection ajax-server

ajax-connection : src/server/AJAXConnection.java
	javac -d bin/server -cp bin/server $<

ajax-server : src/server/AJAXServer.java
	javac -d bin/server -cp bin/server $<
