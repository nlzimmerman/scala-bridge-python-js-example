# scala-bridge-python-js-example
A worked example calling Python and JavaScript from inside a Scala app.

# Motivation
I needed to make some pretty complicated Python interact with some fairly simple
JavaScript, and then do a grid search over some parameters. (It's not that
interesting of a story.) But, since I know [Spark](http://spark.apache.org/)
pretty well and since every problem looks like a nail when you're holding a
hammer, I decided to try to use Spark to parallelize the grid search.

I spent some time messing around with various Python → JS bridges which will
remain nameless because it's probably my fault I couldn't get them to work to my
satisfaction. Then I remembered that modern JVMs come with a JS interpreter built right into it, and decided to see if I could use that and Jython to get everything working. The JS was easy as pie, the Jython was a bit tricker. This is a simple example that demonstrates that things more or less work.

# Disclaimers
- I'm an enthusiastic novice at Scala.
- Before this project I knew nothing about Maven.
- There's very little novel here.
- As of this writing, I haven't actually tested this with Spark. :)
- This isn't a framework, it's just an example you can fork and run with.
  + Which explains why there's some dumb smoke tests but no unit tests.

# What works
- Python integration via [Jython](http://www.jython.org/)
- External python modules that can be installed with easy_install are brought in automatically, with [jython-compile-maven-plugin](http://mavenjython.sourceforge.net/compile/). **This is the secret sauce here, and this is why I'm using maven and not sbt.**
- Python modules you wrote yourself can be dropped into `src/main/resources/python/Lib/`.

# What doesn't work
- Anything that requires CPython will of course not work with Jython. This means no numpy, and nothing that depends on numpy. [JyNI](http://jyni.org/) exists. I haven't tried it; for our case, using straight Jython is a feature, not a bug.
- Right now, everything is straight JavaScript, not Node.JS: we're just reading files and feeding them into the interpreter. NPM isn't going to work; `require` won't either. Sorry, but I'm not really a JavaScript person, and this was all I needed for this project.
