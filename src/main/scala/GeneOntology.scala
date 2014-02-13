package ohnosequences.bio4j.bundles

import shapeless._
import shapeless.ops.hlist._
import ohnosequences.typesets._
import ohnosequences.statika._
import ohnosequences.statika.aws._
import ohnosequences.statika.ami._
import ohnosequences.bio4j.statika._
import ohnosequences.awstools.s3._
import ohnosequences.awstools.regions._
import com.ohnosequences.bio4j.titan.programs._
import java.io._


case object GeneOntologyRawData 
  extends RawDataBundle("http://archive.geneontology.org/latest-termdb/go_daily-termdb.obo-xml.gz")

case object GeneOntologyAPI extends APIBundle(){}

case class GeneOntologyProgram(
  data : File, // 1. Gene ontology xml file
  db   : File  // 2. Bio4j DB folder
) extends ImporterProgram(new ImportGeneOntologyTitan(), Seq(
  data.getAbsolutePath, 
  db.getAbsolutePath
))

case object GeneOntologyImportedData extends ImportedDataBundle(
    rawData = GeneOntologyRawData :~: ∅,
    initDB = InitialBio4j,
    importDeps = ∅
  ) {
  override def install[D <: AnyDistribution](d: D): InstallResults = {
    GeneOntologyProgram(
      data = GeneOntologyRawData.inDataFolder("go.xml"),
      db   = dbLocation
    ).execute ->-
    success("Data " + name + " is imported to" + dbLocation)
  }
}

case object GeneOntologyModule extends ModuleBundle(GeneOntologyAPI, GeneOntologyImportedData)

case object GeneOntologyMetadata extends generated.metadata.GeneOntologyModule()

case object GeneOntologyRelease extends ReleaseBundle(
  ObjectAddress("bio4j.releases", 
                "Gene_Ontology/v" + GeneOntologyMetadata.version.stripSuffix("-SNAPSHOT")), 
  GeneOntologyModule
)

case object GeneOntologyDistribution extends DistributionBundle(
  GeneOntologyRelease,
  destPrefix = new File("/media/ephemeral0/")
)

