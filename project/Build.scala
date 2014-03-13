import sbt._
import sbt.Keys._
import com.github.retronym.SbtOneJar
import com.typesafe.sbteclipse.core.EclipsePlugin.EclipseKeys._
import com.typesafe.sbteclipse.core.EclipsePlugin._
import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import scalariform.formatter.preferences._

object Build extends sbt.Build {
   
   // This is to make sure nobody tries to compile with 1.6 as the target JDK. 
   // Not clear if this will actually work on 1.8, needs to be tested when that is out.
   val specVersion = sys.props("java.specification.version")
   val mismatchedSpecificationMessage = 
   """|Java 1.7 is required for building Scalariform.
      |
      |This is due to a dependency on the javax.swing library, which
      |had an API change from 1.6 to 1.7.
      |
      |Using 1.7 to build requires setting SBT to use JDK 1.7 or higher -- if SBT is
      |booting on JDK 1.6, you will get a javax.swing related compilation error.""".stripMargin
   assert(specVersion == "1.7", mismatchedSpecificationMessage)

  lazy val commonSettings = Defaults.defaultSettings ++ SbtScalariform.defaultScalariformSettings ++ Seq(
    organization := "com.sandinh",
    version := "0.1.5",
    scalaVersion := "2.10.3",
    crossScalaVersions := Seq(
      "2.11.0-RC1",
      "2.10.0", "2.10.1"
    ),
    exportJars := true, // Needed for cli oneJar
    retrieveManaged := true,
    scalacOptions := Seq("-deprecation", "-feature"),
    EclipseKeys.withSource := true,
    EclipseKeys.eclipseOutput := Some("bin"))

  lazy val subprojectSettings = commonSettings ++ Seq(
    ScalariformKeys.preferences <<= baseDirectory.apply(getScalariformPreferences))

  def getScalariformPreferences(dir: File) =
    PreferencesImporterExporter.loadPreferences((dir / ".." / "formatterPreferences.properties").getPath)

  lazy val root: Project = Project("root", file("."), settings = commonSettings ++ Seq(
    publish := (),
    publishLocal := ())) aggregate (scalariform, cli, misc)

  implicit class Regex(sc: StringContext) {
    def r = new util.matching.Regex(sc.parts.mkString, sc.parts.tail.map(_ ⇒ "x"): _*)
  }

  val scalaTestDependency = "org.scalatest" %% s"scalatest"  % "2.1.0" % "test"

  def get2_11Dependencies(scalaVersion: String): List[ModuleID] = scalaVersion match {
    case r"2.11.0.*" ⇒ List(
      "org.scala-lang.modules" %% "scala-xml" % "1.0.0",
      "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.0"
    )
    case _ ⇒ Nil
  }

  lazy val scalariform: Project = Project("scalariform", file("scalariform"), settings =
    subprojectSettings ++ sbtbuildinfo.Plugin.buildInfoSettings ++ eclipseSettings ++
      Seq(
        libraryDependencies <<= (scalaVersion, libraryDependencies) { (sv, deps) ⇒
          deps ++ get2_11Dependencies(sv) :+ scalaTestDependency
        },
        testOptions in Test += Tests.Argument("-oI"),
        pomExtra := pomExtraXml,
        publishMavenStyle := true,
        publishArtifact in Test := false,
        pomIncludeRepository := { _ ⇒ false },
        sbtbuildinfo.Plugin.buildInfoKeys := Seq[sbtbuildinfo.Plugin.BuildInfoKey](version),
        sbtbuildinfo.Plugin.buildInfoPackage := "scalariform",
        sourceGenerators in Compile <+= sbtbuildinfo.Plugin.buildInfo,
        EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Managed,
        publishTo <<= isSnapshot(getPublishToRepo)))

  def getPublishToRepo(isSnapshot: Boolean) =
    if (isSnapshot)
      Some("snapshots" at "https://oss.sonatype.org/content/repositories/snapshots")
    else
      Some("releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2")

  lazy val cli = Project("cli", file("cli"), settings = subprojectSettings ++ SbtOneJar.oneJarSettings ++
    Seq(
      libraryDependencies += "commons-io" % "commons-io" % "1.4",
      mainClass in (Compile, packageBin) := Some("scalariform.commandline.Main"),
      artifactName in SbtOneJar.oneJar := { (version: ScalaVersion, module: ModuleID, artifact: Artifact) ⇒ "scalariform.jar" },
      publish := (),
      publishLocal := ())) dependsOn scalariform

  lazy val misc: Project = Project("misc", file("misc"), settings = subprojectSettings ++
    Seq(
      libraryDependencies ++= Seq(
        "commons-io" % "commons-io" % "1.4",
        "com.miglayout" % "miglayout" % "3.7.4"),
      publish := (),
      publishLocal := (),
      mainClass in (Compile, run) := Some("scalariform.gui.Main"))) dependsOn (scalariform, cli)

  def pomExtraXml =
    <inceptionYear>2010</inceptionYear>
    <url>http://github.com/mdr/scalariform</url>
    <licenses>
      <license>
        <name>MIT License</name>
        <url>http://www.opensource.org/licenses/mit-license.php</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:giabao/scalariform.git</url>
      <connection>scm:git:git@github.com:giabao/scalariform</connection>
    </scm>
    <developers>
      <developer>
        <id>mdr</id>
        <name>Matt Russell</name>
        <url>https://github.com/mdr/</url>
      </developer>
      <developer>
        <id>daniel-trinh</id>
        <name>Daniel Trinh</name>
        <url>https://github.com/daniel-trinh/</url>
      </developer>
      <developer>
        <id>giabao</id>
        <name>Gia Bao</name>
        <url>https://chanphom.com</url>
      </developer>
    </developers>
}