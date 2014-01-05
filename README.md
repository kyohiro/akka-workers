Akka Workers 
===========

Testing the usage of akka in scala.

The basic pattern is one primary master node (manager) controls many slave nodes (workers) to work on some time consuming tasks.

The dispatch approach is worker proactively pulling tasks from manager. Manager only notifies workers that tasks are available from clients. So there would not be any polling.

The first step is to test in single JVM. Later will also try akka-cluster.

