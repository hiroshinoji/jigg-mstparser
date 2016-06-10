import AssemblyKeys._

assemblySettings

organization := "com.github.mynlp"

name := "jigg-mstparser"

scalaVersion := "2.11.7"

version := "0.1-SNAPSHOT"

fork in run := true

parallelExecution in Test := false

crossPaths := false

scalacOptions ++= Seq("-deprecation", "-feature")

libraryDependencies ++= Seq(
  "com.github.mynlp" % "jigg" % "0.6-SNAPSHOT",
  "org.scala-lang.modules" %% "scala-xml" % "1.0.5",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test",
  "net.sourceforge.mstparser" % "mstparser" % "0.5.1"
)

publishMavenStyle := true

publishTo <<= version { (v: String) =>
    val nexus = "https://oss.sonatype.org/"
    if (v.trim.endsWith("SNAPSHOT"))
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

resolvers ++= Seq(
  "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
)

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra := <url>https://github.com/mynlp/jigg</url>
  <licenses>
    <license>
      <name>Apache 2</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0.html</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git@github.com:nozyh/jigg-berkeley-parser.git</url>
    <connection>scm:git:git@github.com:nozyh/jigg-berkeley-parser.git</connection>
  </scm>
  <developers>
    <developer>
      <id>h.nouji@gmail.com</id>
      <name>Hiroshi Noji</name>
      <url>https://github.com/nozyh/</url>
    </developer>
  </developers>
