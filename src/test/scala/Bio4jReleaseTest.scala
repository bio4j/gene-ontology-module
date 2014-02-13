package ohnosequences.bio4j.tests

import java.io._
import org.scalatest._

import ohnosequences.statika.cli.StatikaEC2._
import ohnosequences.awstools.ec2._
import ohnosequences.awstools.ec2.{Tag => Ec2Tag}

import ohnosequences.typesets._
import ohnosequences.statika._
import ohnosequences.statika.aws._
import ohnosequences.statika.ami._
import ohnosequences.bio4j.statika._
import ohnosequences.bio4j.bundles._ 
import ohnosequences.awstools.regions._
import com.ohnosequences.bio4j.titan.model.util._

// Test bundles
object TestBundles {

  def checkGorilla(nodeRetriever: NodeRetrieverTitan): InstallResults = {
    // FIXME: write an actual test
    failure("NO TEST!")
  }

  case object TestBundle 
    extends ReleaseBundle(GeneOntologyRelease.s3address, GeneOntologyRelease.module) {
      override def install[D <: AnyDistribution](d: D): InstallResults = {
        checkGorilla(new NodeRetrieverTitan(
            new Bio4jManager(GeneOntologyRelease.module.dbLocation.getAbsolutePath))) -&-
        GeneOntologyRelease.install(d) -&-
        GeneOntologyDistribution.install(d) -&-
        checkGorilla(GeneOntologyDistribution.nodeRetriever)
      }
  }

  case object TestApplicator extends AWSDistribution(
    GeneOntologyMetadata,
    amzn_ami_pv_64bit(Region.Ireland)(javaHeap = 6),
    members = TestBundle :~: ∅
  )

}

class ApplicationTest extends FunSuite with ParallelTestExecution {

  import TestBundles._

  // for running test you need to have this file in your project folder
  val ec2 = EC2.create(new File("Intercrossing.credentials"))

  val dist = TestApplicator

  def testBundle[B <: AnyBundle : dist.isMember : dist.isInstallable](bundle: B) = {
    test("Apply "+bundle.name+" bundle to an instance"){
      val userscript = dist.userScript(bundle, RoleCredentials)
      // println(userscript)

      val specs = InstanceSpecs(
          instanceType = InstanceType.m1_large
        , amiId = dist.ami.id
        , keyName = "statika" 
        , userData = userscript
        , instanceProfile = Some("bio4j-releaser")
        )

      ec2.applyAndWait(bundle.name, specs, 1) match {
        case List(inst) => assert(inst.getTagValue("statika-status") == Some("success"))
        case _ => assert(false)
      }
    }
  }
  
  testBundle(TestBundle)

}
