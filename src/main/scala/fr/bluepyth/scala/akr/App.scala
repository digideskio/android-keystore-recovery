/**
 * Android Keystore Recovery
 * Copyright (C) 2013 Romain Sertelon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package fr.bluepyth.scala.akr

import akka.actor._
import akka.routing._
import scala.concurrent.duration._
import java.io.FileInputStream
import fr.bluepyth.scala.akr.cli.AKRConfig
import fr.bluepyth.scala.akr.actor._
import fr.bluepyth.scala.akr.jks.JKSUtils
import fr.bluepyth.scala.akr.message._
import fr.bluepyth.scala.akr.generator.SimplePasswordGenerator
import scopt.OptionParser
import java.io.File

/**
 * @author BluePyth
 */
object App {

  def main(args: Array[String]) {

    val parser = new OptionParser[AKRConfig]("AKR") {
        head("AKR", "1.1.0")
        opt[String]("to")                   abbr("t")   action { (v, c) => c.copy(to                 = Some(v)) } text("stop at given password")
        opt[String]("from")                 abbr("f")   action { (v, c) => c.copy(from               = Some(v)) } text("start at given password")
        arg[File]  ("keystore")                         action { (v, c) => c.copy(keystore           = Some(v)) } text("the keystore that will be bruteforced")
        opt[Int]   ("min-length")           abbr("l")   action { (v, c) => c.copy(minLength          = Some(v)) } text("start at given length") 
        opt[Unit]  ("lower-case")           abbr("lc")  action { (_, c) => c.copy(lowerCase          = true)    } text("discards upper-case letters")
        opt[Unit]  ("upper-case")           abbr("uc")  action { (_, c) => c.copy(upperCase          = true)    } text("discards lower-case letters")
        opt[Unit]  ("letters-only")         abbr("lo")  action { (_, c) => c.copy(lettersOnly        = true)    } text("use letters only")
        opt[Unit]  ("numbers-only")         abbr("no")  action { (_, c) => c.copy(numbersOnly        = true)    } text("use numbers only")
        opt[String]("extra-characters")     abbr("ec")  action { (v, c) => c.copy(extraCharacters    = Some(v)) } text("add specified characters in combinations")
        opt[Int]   ("passwords-per-second") abbr("pps") action { (v, c) => c.copy(passwordsPerSecond = Some(v)) } text("number of passwords tested per second") 
    }

    parser.parse(args, AKRConfig()) map { config =>
      startBruteForce(config)
    } getOrElse {
      // arguments are bad, usage message will have been displayed
    }
  }

  def startBruteForce(c: AKRConfig) = {

    val inJKSUtils = new FileInputStream(c.keystore.get)
    implicit val jksUtils = new JKSUtils(inJKSUtils, new Array[Char](1))
    inJKSUtils.close

    val system = ActorSystem("bruteforce")

    val loggerActor = system.actorOf(Props[LoggerActor], "logger")
    
    val smallestMailboxRouter =
    	system.actorOf(Props(new TryPasswordActor(loggerActor)).withRouter(SmallestMailboxRouter(Runtime.getRuntime.availableProcessors)), "router")
    
    val passwordGenerator = new SimplePasswordGenerator(c)

    loggerActor ! StartingBruteForce("Starting Brute force of keystore located at " + c.keystore.get)

    while (passwordGenerator.hasNext && !system.isTerminated) {
	  smallestMailboxRouter ! Password(passwordGenerator.next)
	  
	  // I know this is dirty, but it does the job quite well
	  c.passwordsPerSecond.map { mps =>
	    Thread.sleep(1000 / mps)
	  }
    }

    smallestMailboxRouter ! Broadcast(PoisonPill)

    while (!smallestMailboxRouter.isTerminated) {}

    loggerActor ! PoisonPill
  }

}
