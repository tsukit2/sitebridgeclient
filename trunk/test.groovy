@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.5.1' )

import groovyx.net.http.*
import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*

//def http = new HTTPBuilder( 'http://sitebridgeserver.appspot.com' )
http = new HTTPBuilder( 'http://www2.research.att.com/~bs' )
if (System.properties.'http.proxyHost') {
    http.setProxy(System.properties.'http.proxyHost', 
       System.properties.'http.proxyPort' as int, 
       'http')
}

http.request(GET) { req ->
  uri.path = '/C++.html'
  
  //headers.clear()
  
  response.success = { resp ->
     //println resp.entity.content.getClass()
     System.out << resp.allHeaders.inject([:]) { m,h -> 
                       if (h.name != 'Content-Encoding') {
                          m[h.name] = h.value;
                       } 
                       return m 
                    }
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