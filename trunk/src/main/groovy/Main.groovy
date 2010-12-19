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
import java.util.zip.*

// define where is the server
//http = new HTTPBuilder('http://localhost:8080')
http = new HTTPBuilder('http://sitebridgeserver.appspot.com')
endpoint = new HTTPBuilder('http://adeethai.com')

// here is the main logic of the client and that's it
reset()
while(true) {
   // query for the request. this could be null
   def request = query(); 

   // if request found, satisfy it
   if (request) {
      println request
      response = fetchResponse(request)
      satisfy([responseIndex:request.requestIndex, responseDetails:response])
   
   } else {
      // wait a little bit if no request is found
      Thread.currentThread().sleep(1000) 
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
            def requestObj = convertToMapAndArray(json.request)
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

// utility method to convert json object to map and array
private convertToMapAndArray(jsonObj) {
  switch(jsonObj) {
     case List: 
        return jsonObj.inject([]) { l, elem -> l << convertToMapAndArray(elem); l }
     case Map:  
        return jsonObj.inject([:]) { m, entry -> m[entry.key] = convertToMapAndArray(entry.value); m }
     case JSONNull:
        return null
     default:   
        return jsonObj
  }
}

def fetchResponse(request) {
   print "Fetch response...."
   endpoint.request(groovyx.net.http.Method."${request.requestDetails.method}") { req ->
      // set the path to match the math info
      if (request.requestDetails.pathInfo) {
         println "pathInfo = ${request.requestDetails.pathInfo}"
         uri.path = request.requestDetails.pathInfo
      }
      
      // set query string if any
      if (request.requestDetails.query) {
         uri.query = request.requestDetails.query
      }

      // pass on the original request's headers
      headers.clear()
      headers = request.requestDetails.headers.inject([:]) { m,e ->
         if (e.key != 'Host') {
            m[e.key] = e.value
         }
         return m
      }
         

      response.success = { resp ->
         println 'succeeded'
         
         // prepare the body bytes to send. Rezip it if need to
         def bytes = resp.entity.content.bytes
         /*
         if (resp.headers.'Content-Encoding'?.toLowerCase()?.contains('gzip')) {
            def bytearray = new ByteArrayOutputStream()
            def zip = new GZIPOutputStream(bytearray)
            zip << bytes
            zip.finish()
            bytes = bytearray.toByteArray()
         }*/
         
         // finally return the result
         return [status:resp.status,
                 headers:resp.headers.inject([:]) { m,h -> 
                    if (h.name != 'Content-Encoding') {
                       m[h.name] = h.value;
                    } 
                    return m 
                 },
                 bodyBytes:bytes]
         
      }
   }
}


