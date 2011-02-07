@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.5.1' )

import groovyx.net.http.*
import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*
import org.apache.http.client.params.*

//def http = new HTTPBuilder( 'http://sitebridgeserver.appspot.com' )
http = new HTTPBuilder( 'http://localhost:8080' )
/*
if (System.properties.'http.proxyHost') {
    http.setProxy(System.properties.'http.proxyHost', 
       System.properties.'http.proxyPort' as int, 
       'http')
}
*/

//println http.client.params.getParameter(ClientPNames.DEFAULT_HEADERS)
http.client.params.setParameter(ClientPNames.DEFAULT_HEADERS, [])
http.headers = null

http.client.removeRequestInterceptorByClass(ContentEncoding.RequestInterceptor)
//http.client.removeResponseInterceptorByClass(ContentEncoding.ResponseInterceptor)


http.request(GET) { req ->
   uri.path = '/test/sitebridge/index'
  
  headers.clear()
  headers['Host'] = 'flflf'
  headers['Accept'] = null
//  headers['Connection'] = 'ddl'
  //headers['Accept-Encoding'] = 'normal'
  request.removeHeaders('accept')
  println request.allHeaders
            request.headerIterator().each {
               println "Removing Header: ${it.name} = ${it.value}"
               request.removeHeaders(it)
            }
  println request.params.getParameter('accept')
  request.params = new org.apache.http.params.BasicHttpParams()
    
  response.success = { resp ->
     //println resp.entity.content.getClass()
     //System.out << resp.allHeaders.inject([:]) { m,h -> m[h.name] = h.value; m } 
     System.out << resp.entity.content.text
  }
}

/*
http.request(GET,TEXT) { req ->
  uri.path = '/bridgeconsole/satisfy'
  uri.query = [index:1, responseText:'Hello from script']
  
  response.success = { resp, reader ->
     System.out << reader
  }
}

*/