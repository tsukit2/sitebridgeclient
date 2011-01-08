import com.eddy.sitebridgeclient.*

def endpoint = new URL(bridge.endpointURL)
def server = new URL(bridge.serverURL)
def sslToNormal = 
   endpoint.protocol.toLowerCase() == 'https' && server.protocol.toLowerCase() == 'http'

onRequest = { 
   // transform Host header from the bridge to the actual endpoint
   if (request.headers['Host']) {
      request.headers['Host'] = MiscUtility.parseHostnameRFC2732Format(endpoint.host)
   }

   if (request.headers['Referer']) {
      request.headers['Referer'] = request.headers['Referer'].replace(
         bridge.serverURL, bridge.endpointURL)
   }
}


onResponse = { 
   // if it's redirect, make sure the location stay with the bridge server and do no more processing
   if ([301,302].any { response.status == it}) {
      def oldloc = response.headers['Location']
      response.headers['Location'] = response.headers['Location'].replace(bridge.endpointURL, bridge.serverURL)
      println "Redirect: ${oldloc} to ${response.headers['Location']}"
      return
   }

   // transform cookie
   if (response.headers['Set-Cookie']) {
      def cookieValue = response.headers['Set-Cookie']
      response.headers['Set-Cookie'] = (cookieValue instanceof List 
         ? cookieValue.collect { transformCookie(it) }
         : transformCookie(cookieValue))
   }

   // if content type is text/html, make sure to replace all host appears to 
   if (response.headers['Content-Type']?.startsWith('text/html')) {
      def matcher = response.headers['Content-Type'] =~ /charset=(\S+)/
      def charset = matcher ? matcher[0][1] : 'UTF-8'
      def text = new String(response.bodyBytes, charset)
      text = text.replaceAll(
         MiscUtility.parseHostnameRFC2732Format(endpoint.host), 
         MiscUtility.parseHostnameRFC2732Format(server.host))
      response.bodyBytes = text.getBytes(charset)
   }
}

transformCookie = { cookie ->
   cookie.split(';').findAll { 
      def c = it.trim().toLowerCase()
      // remove domain
      !c.startsWith('domain') &&
      // remove secure flag if server is not secure
      (sslToNormal && c != 'secure')
   }.join(';')
}

