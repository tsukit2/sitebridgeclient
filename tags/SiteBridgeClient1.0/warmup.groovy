@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.5.1' )
@Grab(group='net.sf.json-lib', module='json-lib', version='2.3', classifier='jdk15' )

import groovyx.net.http.*
import java.util.concurrent.*
import groovyx.net.http.*
import static groovyx.net.http.ContentType.*
import static groovyx.net.http.ContentType.JSON as HTTPJSON
import static groovyx.net.http.Method.*

//serverURL = 'http://localhost:8080'
serverURL = 'http://sitebridgeserver.appspot.com'

// thread pool to execute anything asynchronously
executor = Executors.newFixedThreadPool(40)

try {
   2.times { warmup("Kick off server #${it}", 40) }
   while(true) {
      warmup("Keep server warm", 5)
   }
} finally {
   executor.shutdown()
}

def createHTTPBuilder(url) {
   def http = new HTTPBuilder(url)
   if (!(url =~ /localhost/) && System.properties.'http.proxyHost') {
      http.setProxy(System.properties.'http.proxyHost', 
                    System.properties.'http.proxyPort' as int, 
                    'http')
   }
   return http
}

def connectToServer(closure) {
   // define where is the server
   def http = createHTTPBuilder(serverURL)
   try {
      closure(http)
   } finally {
      http.shutdown()
   }
}

def warmup(msg,times) {
   print "${msg}...."
   def queue = (1..times).collect {
      executor.submit({
         connectToServer { http ->
            http.request(GET) { req ->
               uri.path = '/bridgeconsole/warmup'
            }
         }
      } as Runnable)
   }
   queue.each { it.get() }
   println "DONE"
}   

