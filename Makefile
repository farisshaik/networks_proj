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

# Usage:  make client NAME=Alice
client:
	$(JAVA) MathClient $(NAME)

# Quick demo: starts the server in the background then launches three clients
demo: compile
	$(JAVA) MathServer &
	sleep 1
	$(JAVA) MathClient Alice &
	$(JAVA) MathClient Bob   &
	$(JAVA) MathClient Carol &
	wait

clean:
	rm -f *.class
