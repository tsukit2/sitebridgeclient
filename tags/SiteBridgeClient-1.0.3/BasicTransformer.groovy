import com.eddy.sitebridgeclient.*

def endpoint = new URL(bridge.endpointURL)
def server = new URL(bridge.serverURL)
def sslToNormal = 
   endpoint.protocol.toLowerCase() == 'https' && server.protocol.toLowerCase() == 'http'

onRequest = { 
   // transform Host header from the bridge to the actual endpoint
   def hostKey = MiscUtility.getHeaderKeyLike(request.headers, 'Host')
   if (hostKey) {
      request.headers[hostKey] = getHost(endpoint)
   }

   def refererKey = MiscUtility.getHeaderKeyLike(request.headers, 'Referer')
   if (refererKey) {
      request.headers[refererKey] = request.headers[refererKey].replace(
         bridge.serverURL, bridge.endpointURL)
   }
}


onResponse = { 
   // if it's redirect, make sure the location stay with the bridge server 
   if ([301,302].any { response.status == it}) {
      def locKey = MiscUtility.getHeaderKeyLike(response.headers, 'Location')
      def oldloc = response.headers[locKey]
      response.headers[locKey] = response.headers[locKey].replace(bridge.endpointURL, bridge.serverURL)
      println "Redirect: ${oldloc} to ${response.headers[locKey]}"
   }

   // transform cookie
   def cookieKey = MiscUtility.getHeaderKeyLike(response.headers, 'Set-Cookie')
   if (cookieKey) {
      def cookieValue = response.headers[cookieKey]
      response.headers[cookieKey] = (cookieValue instanceof List 
         ? cookieValue.collect { transformCookie(it, sslToNormal) }
         : transformCookie(cookieValue, sslToNormal))
   }

   // if content type is text/html, make sure to replace all host appears to 
   def contentKey = MiscUtility.getHeaderKeyLike(response.headers, 'Content-Type')
   if (contentKey && response.headers[contentKey]?.startsWith('text')) {
      def matcher = response.headers[contentKey] =~ /charset=(\S+)/
      def charset = matcher ? matcher[0][1] : 'UTF-8'
      def text = new String(response.bodyBytes, charset)
      text = text.replaceAll(getHost(endpoint), getHost(server))
      response.bodyBytes = text.getBytes(charset)
   }
}

def transformCookie(cookie, sslToNormal) {
   def ck = cookie.split(';').findAll { 
      def c = it.trim().toLowerCase()
      switch(c) {
         case ~/domain.*/:
         case { sslToNormal && c == 'secure' }:
            return false

         default:
            return true
      }
   }.join(';')
   println "**** ${cookie} ////// ${ck}"
   return ck
}

def getHost(url) {
   def host = url.host
   "${url.host}${url.port != -1 ? ':' + url.port : ''}".toString()
}

