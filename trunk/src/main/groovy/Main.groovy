@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.5.1' )
@Grab(group='net.sf.json-lib', module='json-lib', version='2.3', classifier='jdk15' )

import net.sf.json.*
import groovyx.net.http.*
import static groovyx.net.http.ContentType.*
import static groovyx.net.http.ContentType.JSON as HTTPJSON
import static groovyx.net.http.Method.*
import java.util.zip.*
import org.apache.http.client.RedirectHandler
import java.util.concurrent.*

serverURL = 'http://localhost:8080'
//serverURL = 'http://sitebridgeserver.appspot.com'
endpointURL = args.size() ? args[0] : 'http://www2.research.att.com/~bs'


transformerClasses = loadTransformerClasses([
   '/Users/eddy/Development/googleapp/sitebridgeclient/BasicTransformer.groovy',
   '/Users/eddy/Development/googleapp/sitebridgeclient/BjarneTransformer.groovy'
   ])

// thread pool to execute anything asynchronously
executor = Executors.newCachedThreadPool() //newFixedThreadPool(60)

try { 
   doit()
} finally {
   executor.shutdown()
}

def doit() {
   // here is the main logic of the client and that's it
   reset()
   while(true) {
      // query for the requests. this return list of requests which could be empty
      def requests = query(); 

      // if request found, satisfy it
      if (requests) {
         executor.submit( {
            // get responses asynchronously
            def responses = requests.collect { request ->
               executor.submit({
                  try {
                  // prepare transformer
                  def transformers = transformerClasses*.newInstance()
                  transformers.each {
                     it.binding = new Binding([
                        bridge:[serverURL:serverURL, endpointURL:endpointURL],
                        request:request.requestDetails
                        ])
                  }
                  transformers*.run()

                  transformers*.binding*.onRequest*.call()
                  println request


                  def response = fetchResponse(request)

                  transformers*.binding*.response = response
                  transformers*.binding*.onResponse*.call()

                  println "${response.status}\n${response.headers}"
                  return [responseIndex:request.requestIndex, responseDetails:response]
                  } catch(ex) { ex.printStackTrace() }
               } as Callable<Object>)
            }

            // get all the results (wait for it) and satisfy it
            satisfy(responses.collect { it.get() })
         } as Runnable)
      } else {
         // wait a little bit if no request is found
         Thread.currentThread().sleep(1000) 
      }
   }
}

def loadTransformerClasses(files) {
   def classLoader = new GroovyClassLoader()
   files.collect { classLoader.parseClass(it as File) }
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

def connectToEndPoint(closure) {
   def endpoint = createHTTPBuilder(endpointURL)
   endpoint.client.redirectHandler = [isRedirectRequested: { resp, ctx -> false }] as RedirectHandler
   try {
      closure(endpoint)
   } finally {
      endpoint.shutdown()
   }
}

def reset() {
   print "Resetting server...."
   1.downto(0) { count ->
      connectToServer { http ->
         http.request(GET,HTTPJSON) { req ->
            uri.path = '/bridgeconsole/reset'

            response.success = { resp, json ->
               if (!json.status) {
                  println 'failed'
                  throw new RuntimeException("server reset failed: " + json.status)
               }
               print "...${count}"
            }
         }
      } 
   }
   println '...succeeded'
}

def query() {
   print "Querying server...."
   connectToServer { http ->
      http.request(GET,HTTPJSON) { req ->
         uri.path = '/bridgeconsole/query'

         response.success = { resp, json ->
            //print ".... ${json.request.getClass()} - ${json}...."
            def requests = convertToMapAndArray(
               JSONArray.fromObject(inflateByteArrayToObj(convertToMapAndArray(json.payload) as byte[])))
            if (requests) {
               println "found ${requests.size()} request(s)"
               return requests
            }
            println 'no pending request'
         }
      }
   }
}

def satisfy(responses) {
   print "Satisyfing: ${responses*.responseIndex}...."
   connectToServer { http ->
      http.request(POST,HTTPJSON) { req ->
         uri.path = '/bridgeconsole/satisfy'
         body = [payload:deflateObjectToByteArray(
                           JSONArray.fromObject(responses).toString())]

         response.success = { resp, json ->
            if (!json.satisfied) {
               println 'failed'
               throw new RuntimeException("satisfied request ${responses*.responseIndex} failed")
            }
            println 'succeeded'
         }
      }
   }
}

private deflateObjectToByteArray(obj) {
   def bytes = new ByteArrayOutputStream()
   def outstream = new ObjectOutputStream(new GZIPOutputStream(bytes))
   outstream.writeObject(obj)
   outstream.close()
   bytes.toByteArray()
}

private inflateByteArrayToObj(bytearray) {
   return new ObjectInputStream(
      new GZIPInputStream(new ByteArrayInputStream(bytearray))).readObject()

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
         headers = request.requestDetails.headers
            
         // create response handler closure and configure for both success and failure
         def responseHandler = { resp ->
            // finally return the result
            println 'Ok'
            def bytes = resp.entity.content.bytes
            return [status:resp.status,
                    headers:resp.headers.inject([:]) { m,h -> 
                       def val = h.value
                       if (h.name == 'Content-Length' && (val as long) != bytes.size()) {
                          val = bytes.size().toString()
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