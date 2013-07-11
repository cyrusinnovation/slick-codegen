import scala.slick.jdbc.codegen
import scala.slick.jdbc.reflect
import scala.slick.migrations._
import DB.session
import com.typesafe.config._

object SampleCodegen {
  val tables = DB.tables
  val tableNames = tables.map(_.table).toList
  val tableMap = tables.map(table => table.table -> table).toMap
  val tableToScalaName = tables.map(table => table.table -> table.scalaname).toMap
  def doGen(ver: String) = {
    DB.database withSession {

      class MyTableGen(schema: codegen.Schema, table: reflect.Table) extends codegen.Table(schema, table) {
        override def entityName = tableToScalaName(name)
        override def columns = {
          val conf = tableMap(name)
          val allColumns = super.columns
          val included:List[codegen.Column] = allColumns.filter(c => conf.included contains c.name)
          val namesToExclude:List[String] = conf.excluded ::: conf.included
          val returner = (included ::: allColumns.filterNot(namesToExclude contains _.name)) take conf.maxcols 
          returner
        }
      }

      List("v" + ver, "latest").foreach {
        version =>
          val pkg =  DB.pkg + "." + version + ".schema"
          val generator = new codegen.Schema(
            new scala.slick.jdbc.reflect.Schema(tableNames),
            pkg) {
            override def table(t: reflect.Table) = new MyTableGen(this, t)
            override def render = super.render + s"""
package $pkg.version{
  object Version{
    def version = $ver
  }
}
"""
          }
          val folder = DB.genfolder
          generator.singleFile(folder)
      }
    }
  }
  def gen(mm: MyMigrationManager) {
    if (mm.notYetAppliedMigrations.size > 0) {
      println("Your database is not up to date, code generation denied for compatibility reasons. Please update first.")
      return
    }
    doGen(mm.latest.toString)
  }
}