package com.eddy.sitebridgeclient

import groovyx.net.http.*
import java.util.zip.*
import net.sf.json.*
import org.apache.http.impl.client.*
import org.apache.commons.httpclient.*
import org.apache.commons.httpclient.params.*
import org.apache.http.client.RedirectHandler
import org.apache.http.message.* 
import org.apache.log4j.*
import static groovyx.net.http.ContentType.*
import static groovyx.net.http.ContentType.JSON as HTTPJSON
import static groovyx.net.http.Method.*

/**
 * Provide the service to interface with the server-side of the sitebridge system.
 * It encapsulate the communication protocol and details such that the outside world
 * doesn't need to know how it's communicated.
 */
class Bridge {
   private static Logger log = Logger.getLogger(this.name)
   private serverURL, endpointURL
   private restricted

   /**
    * Constructor.
    *
    * @param restricted          Flag whether SiteBrite should restrict access.
    * @param serverURL           URL of the sitebridge server.
    * @param endpointURL         URL of the endpoint site to be bridged.
    */
   Bridge(boolean restricted, String serverURL, String endpointURL) throws Exception {
      this.restricted = restricted
      this.serverURL = serverURL
      this.endpointURL = endpointURL
   }

   /**
    * Reset the sitebridge server. Call this method to reset all the book-keeping
    * stuff on the server side. This method should always be called before bridging
    * anything.
    */
   void reset() {
      connectToServer { http ->
         http.request(GET,HTTPJSON) { req ->
            uri.path = '/bridgeconsole/reset'
            if (restricted) {
               uri.query = [restricted:true]
            }
            response.success = { resp, json ->
               // if we get response, make sure we see good status
               if (!json.status) {
                  log.error "Server reset status indicating failure: ${json.status}"
                  throw new RuntimeException("Server reset failed: ${json.status}")
               }

               // rerestricted, check the generated passcode
               if (restricted && !json.bridgePasscode) {
                  log.error "No bridge passcode found for restricted mode"
                  throw new RuntimeException("Server reset failed: no bridgePasscode found")
               }

               // if reach here things are good
               log.info "Reset server succeeded ${restricted ? 'bridgePasscode = ' + json.bridgePasscode : ''}"
            }
         }
      }
   }

   /**
    * Query the sitebridge server to get requests. Call this method to get all pending
    * requests. All requests returned are moved to wait state. This means that the client
    * that obtains the request must satisfy them later.
    *
    * @return List of Map object, each indicating a seaparate request.
    */
   List query() {
      connectToServer { http ->
         http.request(GET) { req ->
            uri.path = '/bridgeconsole/query'
            response.success = { resp ->
               def requests = MiscUtility.convertToMapAndArray(
                  JSONArray.fromObject(MiscUtility.inflateByteArrayToObj(resp.entity.content.bytes)))
               if (requests) {
                  log.info "Query server found ${requests.size()} request(s)"
                  return requests
               }
               log.info 'Query server found no pending request'
            }
         }
      }
   }

   /**
    * Satisfy the requests by using the given responses. This send the given responses
    * to the server so the it can relay them to the actual client.
    *
    * @param responses        List of Map object each indicating a response.
    */
   void satisfy(List responses) {
      connectToServer { http ->
         http.request(POST,HTTPJSON) { req ->
            uri.path = '/bridgeconsole/satisfy'
            requestContentType = 'application/octet-stream'
            body = MiscUtility.deflateObjectToByteArray(JSONArray.fromObject(responses).toString())
            response.success = { resp, json ->
               if (!json.satisfied) {
                  log.error "Satisfying requests: ${responses*.responseIndex} failed: ${json.satisfied}"
                  throw new RuntimeException("satisfied request ${responses*.responseIndex} failed")
               }
               log.info "Satisfying requests: ${responses*.responseIndex} succeeded"
            }
         }
      }
   }

   /**
    * Fetch the response from the actual endpoint site using the given request object.
    * This method relays the information in the request map to the endpoin, then 
    * produce another map representing the response of the client.
    *
    * @param requestObj          Map representing the request.
    *
    * @return Map representing the response from the endpoint.
    */
   Map fetchResponse(Map requestObj) {
      connectToEndPoint { endpoint ->
         def httpMethod = groovyx.net.http.Method."${requestObj.requestDetails.method}"
         endpoint.request(httpMethod) { req ->
            // set the path to match the math info
            if (requestObj.requestDetails.pathInfo) {
               uri.path = requestObj.requestDetails.pathInfo
            }
            
            // set query string if any
            if (requestObj.requestDetails.query) {
               uri.query = requestObj.requestDetails.query
            }

            // set body if params are given
            if (requestObj.requestDetails.params) {
               requestContentType = URLENC 
               body = requestObj.requestDetails.params
            }

            // set the headers originally found in the request. Note that we need to 
            // treat headers a bit lower level because same header can show up multiple
            // times. The facility provided by HTTPBuilder treats it as map which doesn't
            // support this notion
            headers.clear()
            headers['Accept'] = null // remove it. it's fine if it's later set back
            request.allHeaders.each { request.removeHeader(it) }
            requestObj.requestDetails.headers.each { k,v ->
               // note that we don't include Content-Length header. This is because
               // HTTPBuilder will take care of this. If we set the original Content-Length,
               // it will blow things up
               if (k.toLowerCase() != 'content-length') {
                  if (v instanceof List) {
                     headers[k] = v.join(',')
                     //v.each { request.addHeader(new BasicHeader(k, it)) }
                  } else {
                     headers[k] = v
                     //request.addHeader(new BasicHeader(k, v))
                  }
               }
            }
            headers['Connection'] = 'close' // we always override this because we never keep it alive

            // create response handler closure and configure for both success and failure
            // note that the failure here is at the HTTP status code, not the net error
            // this means that the communication still succeeded but at the network level
            def responseHandler = { resp ->
               //log.info "*** Response OBJ: ${resp}, ${resp.entity}"
               // the resp's entity could be null for 304
               def bytes = resp.entity?.content?.bytes ?: new byte[0]
               def responseDetails = [
                  status:resp.status,
                  headers:constructHeadersMap(resp.headers, bytes.size()),
                  bodyBytes:bytes
               ]
               return [responseIndex:requestObj.requestIndex, responseDetails:responseDetails]
            }
            response.success = responseHandler
            response.failure = responseHandler.clone()
         }
      }
   }

   /**
    * Call out to bridge server to keep it warm. Each time this call is made, there is some 
    * random processing going on the server. This helps keep the site warm and the instances
    * backing the server stay alive.
    */
   void warmup() {
      connectToServer { http ->
         http.get(path:'/bridgeconsole/warmup', contentType:ANY) { resp ->
            // do nothing
         }
      }
   }

   private constructHeadersMap(headers, bytesSize) {
      headers.inject([:]) { m,h -> 
         def val = h.value
         // First, it seems like HTTPBuilder has a bug that returns wrong 
         // Content-Length. So we need to check with the actual body bytes
         // and correct it
         if (h.name.toLowerCase() == 'content-length' && (val as long) != bytesSize) {
            val = bytesSize.toString()
         }

         // we create list of header if we found the same header multiple
         // times. This is unfortunately the nature of HTTP protocol
         def existingValue = m[h.name]
         m[h.name] = (existingValue != null 
               ? (m[h.name] instanceof List ? existingValue << val : [existingValue, val])
               : val)
         return m 
      }
   }

   private createHTTPBuilder(url) {
      def http = new HTTPBuilder(url)
      if (!(url =~ /localhost/) && System.properties.'http.proxyHost') {
         http.setProxy(System.properties.'http.proxyHost', 
                       System.properties.'http.proxyPort' as int, 
                       'http')
      }
      return http
   }

   private connectToServer(closure) {
      // define where is the server
      def http = createHTTPBuilder(serverURL)
      try {
         closure(http)
      } finally {
         http.shutdown()
      }
   }

   private connectToEndPoint(closure) {
      def endpoint = createHTTPBuilder(endpointURL)
      endpoint.client.redirectHandler = [isRedirectRequested: { resp, ctx -> false }] as RedirectHandler
      endpoint.client.removeRequestInterceptorByClass(ContentEncoding.RequestInterceptor)
      try {
         closure(endpoint)
      } finally {
         endpoint.shutdown()
      }
   }
}
