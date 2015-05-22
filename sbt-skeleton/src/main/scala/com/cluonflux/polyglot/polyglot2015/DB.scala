package com.cluonflux.polyglot.polyglot2015

import java.util.UUID

import slick.ast.ScalaBaseType
import slick.driver.H2Driver.api._
import slick.lifted.{ProvenShape, ForeignKeyQuery}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Failure}

case class Guid[T](raw: UUID) extends AnyVal

object Guid {
  def apply[T](): Guid[T] = Guid[T](UUID.randomUUID())
  def apply[T](s: String): Guid[T] = Guid[T](UUID.fromString(s))
}

// case class Cage(guid: Guid[Cage], name: String)

trait AnimalsSchema {
  implicit def guidMapper[T] = MappedColumnType.base[Guid[T], String](_.raw.toString, Guid[T](_))

  class Cage(tag: Tag) extends Table[(Guid[Cage], String)](tag, "cages") {
    def guid = column[Guid[Cage]]("guid", O.PrimaryKey)
    def name = column[String]("name")

    def * = (guid, name)
  }

  val cageQuery = TableQuery[Cage]

  class Animal(tag: Tag) extends Table[(Guid[Animal], Guid[Cage], String)](tag, "animals") {
    def guid     = column[Guid[Animal]]("guid", O.PrimaryKey)
    def cageGuid = column[Guid[Cage]]("cage_guid")
    def name     = column[String]("name")

    def fk_cageGuid = foreignKey("fk_animal_cage", cageGuid, cageQuery)(_.guid)

    def * = (guid, cageGuid, name)
  }

  val animalQuery = TableQuery[Animal]
}

object DB extends AnimalsSchema {
  val db = Database.forConfig("polyglot2015")

  val cageGuid = Guid[Cage]()

  val setup = DBIO.seq(
    (cageQuery.schema ++ animalQuery.schema).create,

    cageQuery += (cageGuid, "The big one"),
    animalQuery ++= Seq(
      (Guid[Animal](), cageGuid, "Fluffy the cat"),
      (Guid[Animal](), cageGuid, "Dumbo the elephant")
    )
  )

  def main(args: Array[String]): Unit = {
    try {
      val setupFuture = db.run(setup)
      setupFuture.onComplete {
        case Success(()) =>
          // It's a database!
          val shorties = for {
            animal <- animalQuery
            if animal.name === args(0)
          } yield animal.guid

          val xx = db.run(shorties.result)
          xx.foreach { guids =>
            guids.foreach { guid =>
              println(guid)
            }
          }

        case Failure(t) =>
          throw t
      }
      Thread.sleep(10000)
    } finally {
      db.close()
    }
  }
}