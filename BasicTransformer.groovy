def endpoint = new URL(bridge.endpointURL)
def server = new URL(bridge.serverURL)

onRequest = { 
   // transform Host header from the bridge to the actual endpoint
   if (request.headers['Host']) {
      println "Transform host from ${request.headers['Host']} to ${endpoint.host}"
      request.headers['Host'] = endpoint.host
   }

}


onResponse = { 
   // if it's redirect, make sure the location stay with the bridge server
   if (response.status == 301) {
      response.headers['Location'] = response.headers['Location'].replace(endpoint.host, 
         "${server.host}${server.port != 80 ? ':' + server.port : ''}")
   }

   // if content type is text/html, make sure to replace all host appears to 
   if (response.headers['Content-Type'].startsWith('text/html')) {
      def matcher = response.headers['Content-Type'] =~ /charset=(\S+)/
      def charset = matcher ? matcher[0][1] : 'UTF-8'
      def text = new String(response.bodyBytes, charset)
      text = text.replaceAll(endpoint.host, "${server.host}${server.port != 80 ? ':' + server.port : ''}")
      response.bodyBytes = text.getBytes(charset)
   }
}

