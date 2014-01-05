version := "1.0.0"

scalaVersion := "2.10.2"

scalacOptions ++= Seq("-deprecation", "-feature")

libraryDependencies += "org.scalatest" %% "scalatest" % "1.9.1" % "test"

libraryDependencies += "junit" % "junit" % "4.10" % "test"

libraryDependencies ++= Seq("com.typesafe.akka" %% "akka-actor" % "2.2.3",
    					   "com.typesafe.akka" %% "akka-testkit" % "2.2.3")
    					   
libraryDependencies ++= Seq("org.slf4j" % "slf4j-api" % "1.7.5",
    					   "org.slf4j" % "slf4j-simple" % "1.7.5")