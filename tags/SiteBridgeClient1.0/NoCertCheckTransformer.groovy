
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

}




