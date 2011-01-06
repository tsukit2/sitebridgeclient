
def endpoint = new URL(bridge.endpointURL)
def server = new URL(bridge.serverURL)

// we need to suppress the hostname verification if the endpoint is the development machine
if (endpoint.protocol.toLowerCase() == 'https' && endpoint.host.toLowerCase() == 'localhost') {
   def factory = org.apache.http.conn.ssl.SSLSocketFactory.getSocketFactory()
   factory.setHostnameVerifier([ verify: { Object[] params -> } ] as org.apache.http.conn.ssl.X509HostnameVerifier)
}

onRequest = {
   // do nothing for now
}

onResponse = { 
   // transform cookie
   if (response.headers['Set-Cookie']) {
      def cookieValue = response.headers['Set-Cookie']
      response.headers['Set-Cookie'] = (cookieValue instanceof List 
         ? cookieValue.collect { transformCookie(it) }
         : transformCookie(cookieValue))
   }

}

transformCookie = { cookie ->
   switch(cookie) {
      case ~/MBASESSIONID.*/:
      case ~/SIMS.*/:
      case ~/JSESSIONID.*/:
         return (server.protocol.toLowerCase() == 'https' 
            ? cookie
            : cookie.split(';').findAll { it.trim().toLowerCase() != 'secure' }.join(';'))

      case ~/mba_cookie_prod.*/:
         return cookie.split(';').findAll { !it.trim().startsWith('domain') }.join(';')
         //return cookie.replaceAll(/domain=\S*/, "domain=.${server.host}") 

      default:
         return cookie
   }
}



