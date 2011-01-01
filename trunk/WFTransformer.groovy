def endpoint = new URL(bridge.endpointURL)
def server = new URL(bridge.serverURL)

onRequest = {



}

onResponse = { 
   // replace cookie's domain
   if (response.headers['Set-Cookie']) {



   }

}


