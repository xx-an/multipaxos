#!/bin/bash
# State machine replication

sbt package

cp -f target/scala-2.11/cmpaxos_2.11-1.0.jar ~/Documents/Lab/multipaxos1/cmpaxos.jar

cp -f target/scala-2.11/cmpaxos_2.11-1.0.jar ~/Documents/Lab/multipaxos2/cmpaxos.jar

cp -f target/scala-2.11/cmpaxos_2.11-1.0.jar ~/Documents/Lab/multipaxos3/cmpaxos.jar

gnome-terminal -t "replica0" -x bash -c "cd ~/Documents/Lab/multipaxos1;java -ea -Djava.util.logging.config.file=myfile -cp lib/*:cmpaxos.jar lsr.paxos.test.map.SimplifiedMapServer 0;exec bash;"

gnome-terminal -t "replica1" -x bash -c "cd ~/Documents/Lab/multipaxos2;java -ea -Djava.util.logging.config.file=myfile -cp lib/*:cmpaxos.jar lsr.paxos.test.map.SimplifiedMapServer 1;exec bash;"

gnome-terminal -t "replica2" -x bash -c "cd ~/Documents/Lab/multipaxos3;java -ea -Djava.util.logging.config.file=myfile -cp lib/*:cmpaxos.jar lsr.paxos.test.map.SimplifiedMapServer 2;exec bash;"

