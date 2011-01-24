@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.5.1' )

import groovyx.net.http.*
import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*


def uri = new groovyx.net.http.URIBuilder( 'http://localhost?a=1' )
println uri.query.a

println (null << 'f')





//def http = new HTTPBuilder( 'http://sitebridgeserver.appspot.com' )
http = new HTTPBuilder( 'https://localhost:7002' )
/*
if (System.properties.'http.proxyHost') {
    http.setProxy(System.properties.'http.proxyHost', 
       System.properties.'http.proxyPort' as int, 
       'http')
}
*/

http.request(GET) { req ->
   uri.path = '/mba'
  
  //headers.clear()
  
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