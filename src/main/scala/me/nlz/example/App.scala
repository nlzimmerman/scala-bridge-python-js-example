package me.nlz.example

// Lets us do Scala stuff with Java iterables.
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
  /* The name of the path that we are reading directly into the interpreter,
   * relative to src/main/resources
   */
  val loadFilePath: String = "/python/test.py"
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

  /* The following reads a .py file (with a path specified relative to
   * src/main/resources) directly into the interpreter. You may not need or
   * want to do this! If your code is in modules, it might be preferable to do
   * something like
   * p.exec("from package_name.module_name import ClassName")
   * val ClassInstance: PyObject = p.get("ClassName").__call__()
   */

  // we are reading the file into a string
  val stream : InputStream = getClass.getResourceAsStream(loadFilePath)
  val lines: String = scala.io.Source.fromInputStream(stream).getLines().mkString("\n")
  /*
   * compile the code into a code block, then run it to get it into the environment.
   * It would be just fine to just run the string, i.e.
   * p.exec(lines)
   * would work fine.
   */
  val code: PyCode = p.compile(lines)
  p.exec(code)
  // Get the Python class object
  val test: PyObject = p.get("Test")
  // instantiate it. We have to use the double-underscore to do that here. No problem.
  val testInstance: PyObject = test.__call__()
  def plusTwo(x: Int): Int = {
    /* To call a class method from a class object, the syntax is
     * classObject.invoke("methodName", arg0, arg1, arg2)
     * The params have to be PyObjects, not Scala primitives, but we can use
     * Py.java2py() to do that conversion for us.
     * The resut will also be of type PyObject, but we can use the builtins
     * conversion methods to get this back into Java objects.
     * But of course, we have to know the result type for that to work!
     * The potential for runtime errors is pretty large.
     */
    testInstance.invoke("plusTwo", Py.java2py(x)).asInt()
    // we converted the pyObject to a Java Int there at the end.
  }
  // I just did this to convince myself I knew how to manipulate times.
  def rightNow(): LocalDateTime = {

    val v: List[Int] = testInstance.
              // returns a python datetime.datetime
              invoke("rightNow").
              /* datetime.dateime has a method timetuple that creates a nine-item
               * tuple, so let's run it.
               */
              invoke("timetuple").
              /* this turns a PyObject (which is a Python Tuple of ints, not that
               * Jython can know this into a java.lang.Iterable[PyObject]
               * i.e., it does _not_ turn the inner objects from PyObject to Int.
               */
              asIterable().
              // Make a scala list so that we can use map.
              toList.
              // turn the List[PyObject] into a List[Int]
              map(
                (x: PyObject) => x.asInt()
              )
    /* sure there's some way to unpack these arguments. Did I mention I'm kind
     * of new to Scala?
     */
    LocalDateTime.of(v(0),v(1),v(2),v(3),v(4),v(5))
  }
  /* Another silly demonstration that uses the isodate python moduleTest
   * to parse an ISO 8601 duration string to a java.time.Duration.
   * While the underlying function tries to play nice with Months
   * and years, this wrapper doesn't interact with that functionality.
   */
  def parseDuration(x: String): Duration = {
    val v: Double = testInstance.
              invoke("parseDurationString", Py.java2py(x)).
              invoke("total_seconds").
              asDouble()
    Duration.ofSeconds(v.toLong)
  }
  // a wrapper function for a Python function that requires a module import to work
  def moduleTest(): Int = {
    testInstance.invoke("moduleTest").asInt()
  }

}
// a simple scala function for our demo
object ScalaRunner {
  def minusOne(x: Int): Int = {
    x-1
  }
}

/* Our JavaScript runner class. There's a lot less going on here then with the
 * python runner. There's no package support here, we're just defining functions
 * by reading files and stuffing them into the interpreter, then calling those
 * functions that live in the interpreter namespace.
 */
object JSRunner {
  // The file we are going to load which defined our JS functions.
  val loadFilePath: String = "/js/test.js"
  /* from the Oracle Nashorn docs, hand converted Java to Scala
   * http://www.oracle.com/technetwork/articles/java/jf14-nashorn-2126515.html
   * I have no opinions about JavaScript engines; this is the first thing I
   * tried and it worked.
   */
  // Initialize a JS engine.
  val engineManager: ScriptEngineManager = new ScriptEngineManager();
  val engine: ScriptEngine = engineManager.getEngineByName("nashorn");
  // Read our JS file into a string.
  val stream : InputStream = getClass.getResourceAsStream(loadFilePath)
  val lines: String = scala.io.Source.fromInputStream(stream).getLines().mkString("\n")
  // feed that string into the engine to define all the functions.
  engine.eval(lines)
  // This is straight from the documentation linked above.
  val invocable: Invocable = engine.asInstanceOf[Invocable]
  // Scala wrapper to the underlying JS function.
  def plusSix(x: Int): Int = {
    /* There's a nice analogy to the Jython engine here. You invoke functions with
     * invokable.infokeFunction("functionName", arg0, arg1, arg2)
     * both the inputs and outputs are java.lang.Object, so cast input to Object
     * and cast output to whatever it should be.
     * I haven't looked into how to deal with more complicated objects.
     */
    invocable.
        invokeFunction("plusSix", x.asInstanceOf[Object]).
        // Remember that there are no Ints in JS.
        asInstanceOf[Double].
        intValue()
  }
}

object App {

  def plusSixteen(x: Int): Int = {
    // Needlessly silly, but mixes function wrappers from all languages.
    JSRunner.plusSix(ScalaRunner.minusOne(PythonRunner.plusTwo(JSRunner.plusSix(
            JavaRunner.plusOne(PythonRunner.plusTwo(x))
          ))))
  }
  def main(args : Array[String]) {
    println("Hello World!")
    println("The current UTC time is: " ++ PythonRunner.rightNow.toString)
    println("A common 'nerdy' shibboleth is the number " ++ PythonRunner.moduleTest.toString)
    println("Five days, forty hours, and 100 minutes equals: " ++ PythonRunner.parseDuration("P5DT40H100M").toString)
    println("If Spark is working, the next line should read 10016")
    val conf = new SparkConf().setAppName("Toy Application")
    val sc = new SparkContext(conf)
    val x: RDD[Int] = sc.parallelize(0 to 10000, 100)
    println(x.map(plusSixteen).collect().toList.last)
  }
}
