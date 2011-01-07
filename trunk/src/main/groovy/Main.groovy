//@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.5.1' )
//@Grab(group='net.sf.json-lib', module='json-lib', version='2.3', classifier='jdk15' )
//@Grab(group='log4j', module='log4j', version='1.2.16')
//@Grab(group='commons-httpclient', module='commons-httpclient-contrib', version='3.0')
import com.eddy.sitebridgeclient.*
import org.apache.log4j.*

// configure log4j
PropertyConfigurator.configure('log4j.properties');

// show usage if the parameters are not given
if (args.size() == 0) {
   println "Main Usage:"
   println "   <serverURL> <endpointURL> [<transformation script>*]"
   println "Alternative:"
   println "   warmup <serverURL>"
   println "   report [<# requests to go back - default all requests>]"
   System.exit(1)
}

// for now hardcode report dir
reportDir = './data'

// now determine what to do
switch (args[0]) {
   case 'warmup': warmup(); break
   case 'report': report(); break
   default      : bridge()
}

def warmup() {
   // check parameters
   if (args.size() < 2) {
      println "ERROR: Please specify the URL for the bridge site"
      System.exit(1)
   }

   // if reach here, treat the second parameter as the url
   // and start the warmup
   def serverURL = args[1]
   def controller = new Controller(serverURL)
   println "Warming up server..."
   controller.startWarmingUp()

   // register the shutdown hook up to finish the controller
   Runtime.runtime.addShutdownHook(
      new Thread({ controller.finish() } as Runnable))
}

def report() {
   // get the number of requests
   def requestNum = null
   if (args.size() >= 2) {
      requestNum = new Integer(args[1])
   }

   def reporter = new Reporter(reportDir)
   reporter.generateReport(requestNum)
}

def bridge() {
   // check parameters
   if (args.size() < 2) {
      println "ERROR: Please specify at least the URL of both endpoint and bridge site"
      System.exit(1)
   }

   // if reach here, extract the parameters and start bridging
   def serverURL = args[0]
   def endpointURL = args[1]
   def transformerScripts = args[2..<args.size()]
   def controller = new Controller(serverURL, endpointURL, transformerScripts, reportDir)
   println "Bridging..."
   controller.startBridging()

   // register the shutdown hook up to finish the controller
   Runtime.runtime.addShutdownHook(
      new Thread({ controller.finish() } as Runnable))
}
