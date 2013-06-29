package scala.slick.jdbc.meta

import java.io.PrintWriter
import scala.slick.jdbc.JdbcBackend

/**
 * Generate Scala code from database meta-data.
 */
object CodeGen {
  def output2(table: MTable)(implicit session: JdbcBackend#Session) : String = {
    var s = ""
    val columns = table.getColumns.list
    val pkeys = table.getPrimaryKeys.mapResult(k => (k.column, k)).list.toMap
    if(!columns.isEmpty) {
      s = s + ("object "+mkScalaName(table.name.name)+" extends Table[")
      if(columns.tail.isEmpty) s = s + (scalaTypeFor(columns.head))
      else s = s + ("(" + columns.map(c => scalaTypeFor(c)).mkString(", ") + ")")
      s = s + ("](\""+table.name.name+"\") {") + "\n"
      for(c <- columns) s = s + output2(c, pkeys.get(c.column))
      s = s + ("  def * = " + columns.map(c => mkScalaName(c.column, false)).mkString(" ~ ")) + "\n"
      s = s + ("}") + "\n"
    }
    s
    
  }

  def output2(c: MColumn, pkey: Option[MPrimaryKey])(implicit session: JdbcBackend#Session) : String = {
    var s = ""
    s = s + ("  def "+mkScalaName(c.column, false)+" = column["+scalaTypeFor(c)+"](\""+c.column+"\"")
    for(n <- c.sqlTypeName) {
      s = s + (", O DBType \""+n+"")
      for(i <- c.columnSize ) s = s + ("("+i+")")
      s = s + ("\"")
    }
    if(c.isAutoInc.getOrElse(false)) s = s + (", O AutoInc")
    for(k <- pkey) s = s + (", O PrimaryKey")
    s = s + (")") + "\n"
    s
  }
  def output(table: MTable, out: PrintWriter)(implicit session: JdbcBackend#Session) {
    val columns = table.getColumns.list
    val pkeys = table.getPrimaryKeys.mapResult(k => (k.column, k)).list.toMap
    if(!columns.isEmpty) {
      out.print("object "+mkScalaName(table.name.name)+" extends Table[")
      if(columns.tail.isEmpty) out.print(scalaTypeFor(columns.head))
      else out.print("(" + columns.map(c => scalaTypeFor(c)).mkString(", ") + ")")
      out.println("](\""+table.name.name+"\") {")
      for(c <- columns) output(c, pkeys.get(c.column), out)
      out.println("  def * = " + columns.map(c => mkScalaName(c.column, false)).mkString(" ~ "))
      out.println("}")
    }
  }

  def output(c: MColumn, pkey: Option[MPrimaryKey], out: PrintWriter)(implicit session: JdbcBackend#Session) {
    out.print("  def "+mkScalaName(c.column, false)+" = column["+scalaTypeFor(c)+"](\""+c.column+"\"")
    for(n <- c.sqlTypeName) {
      out.print(", O DBType \""+n+"")
      for(i <- c.columnSize ) out.print("("+i+")")
      out.print("\"")
    }
    if(c.isAutoInc.getOrElse(false)) out.print(", O AutoInc")
    for(k <- pkey) out.print(", O PrimaryKey")
    out.println(")")
  }

  def mkScalaName(s: String, capFirst:Boolean = true):String = {
    val b = new StringBuilder
    var cap = capFirst
    for(c <- s) {
      if(c == '_') cap = true
      else {
        val allowed = if(b.isEmpty) c.isUnicodeIdentifierStart else c.isUnicodeIdentifierPart
        if(allowed) b append (if(cap) c.toUpper else c.toLower)
        cap = false
      }
    }
    val result = b.toString
    if (scalaKeywords.contains(result)) "m" + mkScalaName(s, true) else result
  }

  def scalaTypeFor(c: MColumn): String =
    if(c.nullable.getOrElse(true)) "Option[" + scalaTypeFor(c.sqlType) + "]" else scalaTypeFor(c.sqlType)

  def scalaTypeFor(sqlType: Int): String = {
    import java.sql.Types._
    sqlType match {
      case BIT | BOOLEAN => "Boolean"
      case TINYINT => "Byte"
      case SMALLINT => "Short"
      case INTEGER => "Int"
      // using "BigInteger" causes
      // could not find implicit value for parameter tm: scala.slick.ast.TypedType[java.math.BigInteger]
      // in generated code.
      case BIGINT => "Long"
      case FLOAT => "Float"
      case REAL | DOUBLE => "Double"
      case NUMERIC | DECIMAL => "BigDecimal"
      case CHAR | VARCHAR | LONGVARCHAR => "String"
      case DATE => "java.sql.Date"
      case TIME => "java.sql.Time"
      case TIMESTAMP => "java.sql.Timestamp"
      case BINARY | VARBINARY | LONGVARBINARY | BLOB => "java.sql.Blob"
      case NULL => "Null"
      case CLOB => "java.sql.Clob"
      case _ => "AnyRef"
    }
  }
  
  val tableMethods = List("clone","column","createFinderBy","create","ddl","eq","equals","foreignKey","foreignKeys","getClass","getLinearizedNodes","getResult","hashCode","index","indexes","isInstanceOf","mapOp","narrowedLinearizer","ne","nodeChildNames","nodeChildren","nodeDelegate","nodeIntrinsicSymbol","nodeMapChildren","nodeShaped","nodeTableSymbol","notify","notifyAll","op","primaryKey","schemaName","setParameter","synchronized","tableConstraints","tableName","toString","updateResult","wait")
  val scalaKeywords = List("wait","abstract","case","catch","class","def","do","else","extends","false","final","finally","for","forSome","if","implicit","import","lazy","match","new","null","object","override","package","private","protected","return","sealed","super","this","throw","trait","try","true","type","val","var","while","with","yield")
}
