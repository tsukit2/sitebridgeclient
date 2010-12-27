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
import org.apache.http.client.RedirectHandler

// here is the main logic of the client and that's it
reset()
while(true) {
   // query for the requests. this return list of requests which could be empty
   def requests = query(); 

   // if request found, satisfy it
   if (requests) {
      requests.each { request ->
         Thread.start {
            println request
            def response = fetchResponse(request)
            println "${response.status}\n${response.headers}"
            satisfy([responseIndex:request.requestIndex, responseDetails:response])
         }
      }
   } else {
      // wait a little bit if no request is found
      Thread.currentThread().sleep(1000) 
   }
}

def connectToServer(closure) {
   // define where is the server
   //def http = new HTTPBuilder('http://localhost:8080')
   def http = new HTTPBuilder('http://sitebridgeserver.appspot.com')
   try {
      closure(http)
   } finally {
      http.shutdown()
   }
}

def connectToEndPoint(closure) {
   def endpoint = new HTTPBuilder('http://en.wikipedia.org')
   endpoint.client.redirectHandler = [isRedirectRequested: { resp, ctx -> false }] as RedirectHandler
   try {
      closure(endpoint)
   } finally {
      endpoint.shutdown()
   }
}

def reset() {
   print "Resetting server...."
   connectToServer { http ->
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
}

def query() {
   print "Querying server...."
   connectToServer { http ->
      http.request(GET,HTTPJSON) { req ->
         uri.path = '/bridgeconsole/query'

         response.success = { resp, json ->
            //print ".... ${json.request.getClass()} - ${json}...."
            def requests = convertToMapAndArray(json)
            if (requests) {
               println "found ${requests.size()} request(s)"
               return requests
            }
            println 'no pending request'
         }
      }
   }
}

def satisfy(responseObj) {
   print "Satisyfing: ${responseObj.responseIndex}...."
   connectToServer { http ->
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
   connectToEndPoint { endpoint ->
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
            
         // create response handler closure and configure for both success and failure
         def responseHandler = { resp ->
            println 'Ok'
            // prepare the body bytes to send. Rezip it if need to
            def bytes = resp.entity.content.bytes
            def bytesCount = bytes.size()
            
            // compress the bytes to speed up transmission
            if (bytes) {
               def bytearray = new ByteArrayOutputStream()
               def zip = new GZIPOutputStream(bytearray)
               zip << bytes
               zip.finish()
               bytes = bytearray.toByteArray()
            }

            println "***** ${bytesCount} / ${bytes.size()}"
            
            // finally return the result
            return [status:resp.status,
                    headers:resp.headers.inject([:]) { m,h -> 
                       def val = h.value
                       if (h.name == 'Content-Length' && (val as long) != bytesCount) {
                          val = bytesCount.toString()
                       }
                       m[h.name] = val;
                       return m 
                    },
                    bodyBytes:bytes]
            
         }
         response.success = responseHandler
         response.failure = responseHandler
      }
   }
}

