package dominus.scala.intg.datastore.mongodb

import com.mongodb.casbah.Imports._
import dominus.framework.junit.DominusJUnit4TestBase
import org.junit.Test
import salat._
import salat.global._
import org.junit.Assert._

/**
 * The Casbah driver provides an idiomatic Scala API that is built on top of the MongoDB Async Java driver.
 * Salat is a bi-directional Scala case class serialization library that leverages MongoDB's DBObject (which uses BSON underneath) as its target format.
 */
class TestMongoDBScalaApi extends DominusJUnit4TestBase {

  var mongoClient: MongoClient = _

  override protected def doSetUp(): Unit = {
    super.doSetUp()
    mongoClient = MongoClient(properties.getProperty("mongodb.connect"), 27017)
  }

  override protected def doTearDown(): Unit = {
    super.doTearDown()
    mongoClient("test-db")("test-coll").remove(MongoDBObject.empty)
    mongoClient.close()
  }

  @Test
  def testCasbahBasicCRUD(): Unit = {
    val coll = mongoClient("test-db")("test-coll")
    coll.insert(MongoDBObject("hello" -> "world"))
    coll.insert(MongoDBObject("language" -> "scala"))
    assertEquals(2, coll.count())
    coll.find().foreach(doc => println(doc))
  }

  @Test
  def testSalatCaseClassCRUD(): Unit = {
    val coll = mongoClient("test-db")("test-coll")
    val dbo = grater[Employee].asDBObject(Employee("shawguo", Option(30), Option(BigDecimal(2000))))
    println(dbo)
    coll.insert(dbo)
    assertEquals(1, coll.count())
    coll.find().foreach(doc => println(doc))
  }
}

case class Employee(name: String, age: Option[Int], annual_salary: Option[BigDecimal])

case class Department(name: String, head_honcho: Option[Employee], cya_factor: BigDecimal, minions: List[Employee])

case class Company(name: String, year_of_inception: Int, departments: Map[String, Department])

