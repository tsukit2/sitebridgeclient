def endpoint = new URL(bridge.endpointURL)
def server = new URL(bridge.serverURL)

onRequest = {



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
         return (server.protocol.toLowerCase() == 'https' 
            ? cookie
            : cookie.split(';').findAll { it.trim() != 'secure' }.join(';'))

      case ~/mba_cookie_prod.*/:
         return cookie.split(';').findAll { !it.trim().startsWith('domain') }.join(';')
         //return cookie.replaceAll(/domain=\S*/, "domain=.${server.host}") 

      default:
         return cookie
   }
}



