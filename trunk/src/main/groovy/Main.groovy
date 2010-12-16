@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.5.0-RC2' )
@Grab(group='net.sf.json-lib', module='json-lib', version='2.3', classifier='jdk15' )

import net.sf.json.*
/*
import net.sf.json.groovy.*;
*/

import groovyx.net.http.*
import static groovyx.net.http.ContentType.*
import static groovyx.net.http.ContentType.JSON as HTTPJSON
import static groovyx.net.http.Method.*

http = new HTTPBuilder( 'http://localhost:8080' )

reset()
while(true) {
   // query for the request. this could be null
   def request = query(); 

   // if request found, satisfy it
   if (request) {
      println request
      satisfy([responseIndex:request.requestIndex,
               responseDetails:[responseText:"Current Time: ${new Date()}".toString()]])
   
   } else {
      // wait a little bit if no request is found
      Thread.currentThread().sleep(3000) 
   }
}

def reset() {
   print "Resetting server...."
   http.request(GET,HTTPJSON) { req ->
      uri.path = '/bridgeconsole/reset'

      response.success = { resp, json ->
         if (!json.status) {
            println 'failed'
            throw new RuntimeException("server reset failed: " + json.status)
         }
         println 'succeeded'
      }
   }
}

def query() {
   print "Querying server...."
   http.request(GET,HTTPJSON) { req ->
      uri.path = '/bridgeconsole/query'

      response.success = { resp, json ->
         //print ".... ${json.request.getClass()} - ${json}...."
         if (!(json.request instanceof JSONNull)) {
            def requestObj = JSONObject.toBean(json.request, Map)
            println "found request ${requestObj.requestIndex}"
            return requestObj
         }
         println 'no pending request'
      }
   }
}

def satisfy(responseObj) {
   print "Satisyfing: ${responseObj.responseIndex}...."
   http.request(POST,HTTPJSON) { req ->
      uri.path = '/bridgeconsole/satisfy'
      body = responseObj

      response.success = { resp, json ->
         if (!json.satisfied) {
            println 'failed'
            throw new RuntimeException("satisfied request ${responseObj.responseIndex} failed")
         }
         println 'succeeded'
      }
   }
}


