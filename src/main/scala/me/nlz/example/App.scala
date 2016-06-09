package me.nlz.example

import scala.collection.JavaConversions._

import java.util.Properties;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.Duration

//Python
import org.python.core.Py;
import org.python.core.imp;
import org.python.util.PythonInterpreter;
import org.python.core.PyObject;
import org.python.core.PyCode;

//JS
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.Invocable;

//spark
import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.rdd.RDD

/**
 * @author Neil L Zimmerman
 */

object PythonRunner {
  /* path to append to the PythonPath, relative to src/main/resources
   * This should be modified to fit your purposes.
   */
  val localModulePath: String = "/python/Lib"

  val p = new PythonInterpreter()
  // copy-pasted without researching what it was for.
  Py.getSystemState().__setattr__("_jy_interpreter", Py.java2py(p))
  /* This may not be the right way to set the Python system path.
   * It does work for me.
   * This gets the real, absolute path of the JAR, as a URI-formatted string,
   * i.e. file:/home/user/path.jar
   * .getLocation would return a URI
   */
  val jarPathURIString: String = getClass.
                                   getProtectionDomain.
                                   getCodeSource.
                                   getLocation.
                                   toString
  // This splits on the ':' character and returns the second half.
  val javaPathString: String = jarPathURIString.split(":")(1)
  /* Now that we have a full path string, we add it to the Python Path
   */
  Py.getSystemState.path.append(Py.java2py(javaPathString ++ localModulePath))
  imp.load("site");
  // we are reading the file into a string
  val stream : InputStream = getClass.getResourceAsStream("/python/test.py")
  val lines: String = scala.io.Source.fromInputStream( stream ).getLines().mkString("\n")
  //
  // compile the code into a code block, then run it to get it into the environment.
  // It would be just fine to just run the string.
  val code: PyCode = p.compile(lines)
  p.exec(code)
  // Get the Python class object
  val test: PyObject = p.get("Test")
  // instantiate it. Got to use the double-underscore to do that here. No problem.
  val testInstance: PyObject = test.__call__()
  def plusTwo(x: Int): Int = {
    //This is how you call a class method when you have a class object.
    testInstance.invoke("plusTwo", Py.java2py(x)).asInt()
    // we converted the pyObject to a Java Int there at the end.
  }
  def rightNow(): LocalDateTime = {

    val v = testInstance.
              invoke("rightNow").
              invoke("timetuple").
              asIterable().
              toList.
              map(
                (x: PyObject) => x.asInt()
              )
    // I'm sure there's some way to unpack these arguments.
    LocalDateTime.of(v(0),v(1),v(2),v(3),v(4),v(5))
  }
  def parseDuration(x: String): Duration = {
    val v: Double = testInstance.
              invoke("parseDurationString", Py.java2py(x)).
              invoke("total_seconds").
              asDouble()
    Duration.ofSeconds(v.toLong)
  }
  def moduleTest(): Int = {
    testInstance.invoke("moduleTest").asInt()
  }

}

object ScalaRunner {
  def minusOne(x: Int): Int = {
    x-1
  }
}

object JSRunner {
  //from the Oracle Nashorn docs, hand converted Java to Scala
  val engineManager: ScriptEngineManager = new ScriptEngineManager();
  val engine: ScriptEngine = engineManager.getEngineByName("nashorn");
  val stream : InputStream = getClass.getResourceAsStream("/js/test.js")
  val lines: String = scala.io.Source.fromInputStream( stream ).getLines().mkString("\n")
  engine.eval(lines)
  val invocable: Invocable = engine.asInstanceOf[Invocable]
  def plusSix(x: Int): Int = {
    invocable.invokeFunction("plusSix", x.asInstanceOf[Object]).asInstanceOf[Double].intValue()
  }
}

object App {

  def plusSixteen(x: Int): Int = {
    JSRunner.plusSix(ScalaRunner.minusOne(PythonRunner.plusTwo(JSRunner.plusSix(
            JavaRunner.plusOne(PythonRunner.plusTwo(x))
          ))))
  }
  def main(args : Array[String]) {
    println( "Hello World!" )
    val conf = new SparkConf().setAppName("Toy Application")
    val sc = new SparkContext(conf)
    val x: RDD[Int] = sc.parallelize(0 to 10000, 100)
    println(x.map(plusSixteen).collect().toList)
  }
}
