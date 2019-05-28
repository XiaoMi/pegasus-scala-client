version := "1.11.4-1-SNAPSHOT"

organization := "com.xiaomi.infra"

name := "pegasus-scala-client"

scalaVersion := "2.12.8"

publishMavenStyle := true

crossPaths := false

resolvers ++= Seq( //额外仓库添加
  "Admonitor Repository" at "http://your-url/",
  "Local Maven Repository" at "file://your-url/"
)

publishTo := {//发布地址
  val nexus = "http://your-url/"
  if (isSnapshot.value) Some("snapshots" at nexus + "snapshots") else Some("releases" at nexus + "releases")
}

// credentials, 路径可以自己指定：
credentials += Credentials(new File((Path.userHome / ".sbt" / ".credentials").toString()))

libraryDependencies ++= Seq(
  "com.google.guava" % "guava" % "20.0",
  "com.xiaomi.infra" % "pegasus-client" % "1.11.4-thrift-0.11.0-inlined",
  "org.scalatest" %% "scalatest" % "3.0.3" % Test
)
