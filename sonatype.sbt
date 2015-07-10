// Your profile name of the sonatype account. The default is the same with the organization value
sonatypeProfileName := "io.github.lemonxah"

// To sync with Maven central, you need to supply the following information:
pomExtra in Global := {
  <url>http://lemonxah.github.io/dbabspro.html</url>
  <licenses>
    <license>
      <name>Apache 2</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
    </license>
  </licenses>
  <scm>
    <connection>scm:git:github.com/lemonxah/dbabspro</connection>
    <developerConnection>scm:git:git@github.com:lemonxah/dbabspro.git</developerConnection>
    <url>github.com/lemonxah/dbabspro</url>
  </scm>
  <developers>
    <developer>
      <id>lemonxah</id>
      <name>Ryno Kotze</name>
      <url>http://lemonxah.github.io</url>
    </developer>
  </developers>
}