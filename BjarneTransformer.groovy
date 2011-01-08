import com.eddy.sitebridgeclient.*

def endpoint = new URL(bridge.endpointURL)
def server = new URL(bridge.serverURL)

onRequest = {
   // fix the path info for + symbol in the url
   if (request.pathInfo?.contains(' ')) {
      request.pathInfo = request.pathInfo.replaceAll(' ', '+')
   }
}

onResponse = { 
   // if content type is text/html, make sure to replace all host appears to 
   if (response.headers['Content-Type']?.startsWith('text/html')) {
      def matcher = response.headers['Content-Type'] =~ /charset=(\S+)/
      def charset = matcher ? matcher[0][1] : 'UTF-8'
      def text = new String(response.bodyBytes, charset)
      text = text.replaceAll('www.research.att.com', 
                             MiscUtility.parseHostnameRFC2732Format(server.host))
      response.bodyBytes = text.getBytes(charset)
   }
}

