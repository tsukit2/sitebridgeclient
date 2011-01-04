@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.5.1' )
@Grab(group='net.sf.json-lib', module='json-lib', version='2.3', classifier='jdk15' )
@Grab(group='log4j', module='log4j', version='1.2.16')
import com.eddy.sitebridgeclient.*
import org.apache.log4j.*

// configure log4j
PropertyConfigurator.configure('log4j.properties');

// show usage if the parameters are not given
if (args.size() == 0) {
   println "Main Usage:"
   println "   <endpointURL> <serverURL> [<transformation script>*]"
   println "Alternative:"
   println "   warmup <serverURL>"
   println "   report [<# requests to go back - default 30>]"
   System.exit(1)
}

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
   println "Report"

}

def bridge() {
   // check parameters
   if (args.size() < 2) {
      println "ERROR: Please specify at least the URL of both endpoint and bridge site"
      System.exit(1)
   }

   // if reach here, extract the parameters and start bridging
   def endpointURL = args[0]
   def serverURL = args[1]
   def transformerScripts = args[2..<args.size()]
   def controller = new Controller(serverURL, endpointURL, transformerScripts, './data')
   println "Bridging..."
   controller.startBridging()

   // register the shutdown hook up to finish the controller
   Runtime.runtime.addShutdownHook(
      new Thread({ controller.finish() } as Runnable))
}
