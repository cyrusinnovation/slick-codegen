package scala.slick.migrations

import scala.slick.driver._
import com.typesafe.config._
import scala.util.control.Exception._
import scala.util.control.{ Exception, NonFatal }
import JdbcDriver._
import language.existentials
import scala.collection.JavaConverters._

case class DB(driver: String, url: String, user: String, password: String) {
  def slickdriver: JdbcDriver = driver match {
    case "org.apache.derby.jdbc.EmbeddedDriver" => DerbyDriver
    case "org.h2.Driver" => H2Driver
    case "org.hsqldb.jdbcDriver" => HsqldbDriver
    case "com.mysql.jdbc.Driver" => MySQLDriver
    case "org.postgresql.Driver" => PostgresDriver
    case "org.sqlite.JDBC" => SQLiteDriver
    case "com.microsoft.sqlserver.jdbc.SQLServerDriver" => SQLServerDriver
    case _ => H2Driver
  }
  def slickdriverimport = "import " + slickdriver.getClass().getName().replaceAll("\\$","") + ".simple._"
  def db = slickdriver.simple.Database.forURL(url, driver = driver, user = user, password = password)
  def session = slickdriver.simple.Database.threadLocalSession
}
object TableConf {
  val catcher = Exception.nonFatalCatch
  def getString(cv:ConfigValue) = catcher.opt(cv.unwrapped.asInstanceOf[String])
  def getInt(cv:ConfigValue) = catcher.opt(cv.unwrapped.asInstanceOf[Int])
  def getListOfStrings(cv:ConfigValue) = {
    catcher.opt(cv.unwrapped.asInstanceOf[java.util.List[String]].asScala.toList)
  }
  def getFromObj(obj:ConfigObject, key:String) = catcher.opt(obj.get(key))
  def getString(obj:ConfigObject, key:String):Option[String] = getFromObj(obj, key).flatMap(getString(_))
  def getInt(obj:ConfigObject, key:String):Option[Int] = getFromObj(obj,key).flatMap(getInt(_))
  def getListOfStrings(obj:ConfigObject, key:String):Option[List[String]] = getFromObj(obj, key).flatMap(getListOfStrings(_))
  def create(obj:ConfigObject) = {
    val maxcols = getInt(obj, "maxcols").getOrElse(22)
    val included = getListOfStrings(obj, "included").getOrElse(List())
    val excluded = getListOfStrings(obj, "excluded").getOrElse(List())
    
    (getString(obj, "table"), getString(obj, "scalaname")) match {
      case (Some(stable), Some(sname)) => Some(TableConf(stable, sname, maxcols, included, excluded))
      case _ => None
    }
  }
}
case class TableConf(table:String, scalaname:String, maxcols:Int, included:List[String], excluded:List[String]) {
  
}
object DB {
  val catcher = Exception.nonFatalCatch
  val conf = ConfigFactory.load
  

  def confOpt(key: String) = catcher.opt(conf.getString(key))

  def dbConfig(item: String) = confOpt("datasource") match {
    case Some(ds) => s"db.$ds.$item"
    case _ => s"db.default.$item"
  }
  def dbOpt(item: String) = confOpt(dbConfig(item))

  def apply(): DB = {
    (dbOpt("driver"), dbOpt("url"), dbOpt("user"), dbOpt("password")) match {
      case (Some(driver), Some(url), Some(user), Some(password)) => DB(driver, url, user, password)
      case (Some(driver), Some(url), _, _) => DB(driver, url, "sa", "")
      case _ => DB("org.h2.Driver", "jdbc:h2:" + System.getProperty("user.dir") + "/test.tb", "sa", "")
    }
  }
  val mydb = DB()

  def database = mydb.db
  def importline = mydb.slickdriverimport
  val driver = mydb.slickdriver
  implicit def session = mydb.session
  
  val mylist:Option[java.util.List[ConfigObject]] = catcher.opt(conf.getObjectList("codegen").asInstanceOf[java.util.List[ConfigObject]])
  
  def tables = mylist match {
    case Some(objlist) => objlist.asScala.map(TableConf.create(_)).flatten
    case _ => TableConf("users", "User", 22, List(), List()) :: Nil
  }
  val genfolder = confOpt("genfolder").getOrElse(System.getProperty("user.dir") + "/src/main/scala")
  val pkg = confOpt("package").getOrElse("datamodel")

}