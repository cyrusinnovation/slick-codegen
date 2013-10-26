import scala.slick.migrations._
object Tool extends App{
  args.toList match{
    case "codegen" :: Nil =>
      Codegen.doGen()
    case "dbdump" :: Nil =>
      import DB.session
      import scala.slick.jdbc.StaticQuery._
      DB.database.withSession{
        println( queryNA[String]("SCRIPT").list.mkString("\n") )
      }
    case _ =>
      println("""
-------------------------------------------------------------------------------
A list of command available in this proof of concept:

  codegen   generate data model code (table objects, case classes) from the
            database schema

  dbdump    print a dump of the current database
-------------------------------------------------------------------------------
""".trim)

  }
}
