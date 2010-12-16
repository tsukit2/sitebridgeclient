@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.5.0-RC2' )

import groovyx.net.http.*
import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*

//def http = new HTTPBuilder( 'http://sitebridgeserver.appspot.com' )
def http = new HTTPBuilder( 'http://localhost:8080' )


http.request(GET,JSON) { req ->
  uri.path = '/bridgeconsole/query'
  
  response.success = { resp, json ->
     System.out << json.requestIndex
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