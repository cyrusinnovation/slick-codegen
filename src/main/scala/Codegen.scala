import scala.slick.jdbc.codegen
import scala.slick.jdbc.reflect
import scala.slick.migrations._
import DB.session

object Codegen {
  val tables = DB.tables
  val tableNames = tables.map(_.table).toList
  val tableMap = tables.map(table => table.table -> table).toMap
  val tableToScalaName = tables.map(table => table.table -> table.scalaname).toMap

  def doGen() = {
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

      val pkg =  DB.pkg + ".schema"
      val generator = new codegen.Schema(
        new scala.slick.jdbc.reflect.Schema(tableNames), pkg) {
        override def table(t: reflect.Table) = new MyTableGen(this, t)
      }
      val folder = DB.genfolder
      generator.singleFile(folder)
    }
  }
}