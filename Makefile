# CS 4390 - Computer Networks | Spring 2026
# Math Server — Makefile
#
# Targets:
#   make            — compile both files
#   make server     — run the server
#   make client     — run a client (NAME=Alice  or auto-generated name)
#   make demo       — compile + start server in background + launch 3 clients
#   make clean      — remove compiled .class files

JAVAC = javac
JAVA  = java

.PHONY: all compile server client demo clean

all: compile

compile:
	$(JAVAC) MathServer.java MathClient.java

server:
	$(JAVA) MathServer

# Usage:  make client NAME=Faris
client:
	$(JAVA) MathClient $(NAME)

# Quick demo: starts the server in the background then launches three clients.
# Expressions are piped into each client to simulate user input automatically.
demo: compile
	$(JAVA) MathServer &
	sleep 1
	printf "10 + 5\n6 * 7\n20 - 8\ndone\n"              | $(JAVA) MathClient Faris  &
	printf "100 / 4\n3 + 4 * 2\n(3 + 4) * 2\ndone\n"    | $(JAVA) MathClient Aariz  &
	printf "50 - 10 / 2\n8 * 8 - 4\n15 + 3 * 4 - 2\ndone\n" | $(JAVA) MathClient Pritam &
	wait

clean:
	rm -f *.class
