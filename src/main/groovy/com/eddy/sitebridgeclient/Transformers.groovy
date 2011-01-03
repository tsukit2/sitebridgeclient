package com.eddy.sitebridgeclient

import org.apache.log4j.*

/**
 * Facilitate the tranformation process of the bridging. This site works closely with Bridge
 * to drive transformers through the request and response. It intimately knows how the 
 * resquest and response are structured though the outside world does not necessarily need
 * to.
 */
class Transformers {
   private static Logger log = Logger.getLogger(this.name)
   private transformers
   private serverURL, endpointURL

   /**
    * Constructor.
    *
    * @param scriptNames            Names of the transformer script.
    * @param serverURL           URL of the sitebridge server.
    * @param endpointURL         URL of the endpoint site to be bridged.
    */
   Transformers(List<String> scriptNames, String serverURL, String endpointURL) {
      // save off the two URL for later use
      this.serverURL = serverURL
      this.endpointURL = endpointURL

      // load up the transformers for later use
      transformers = loadTransformers(scriptNames, 
         [bridge:[serverURL:serverURL, endpointURL:endpointURL]])
   }

   /**
    * Transform request as given in the map.
    * 
    * @param request             Request object to be transformed.
    *
    * @return a map representing the transformation of the request. This object
    *    will be needed in transformResponse method.
    */
   Map transformRequest(Map request) {
      // set up delegate map
      def delegate = [
         bridge:[serverURL:serverURL, endpointURL:endpointURL],
         request:request.requestDetails
      ]

      // log request before transformation
      log.debug "Request before transformation: ${request.requestDetails}"

      // get onRequest handlers and call it
      def onRequestHandlers = transformers*.onRequest*.clone()
      onRequestHandlers*.delegate = delegate
      onRequestHandlers*.call()
      log.info "Finish transforming request"

      // log request after transformation
      log.debug "Request after transformation: ${request.requestDetails}"

      // return the delegate used here as the context
      return delegate
   }

   /**
    * Transform response as given in the map.
    *
    * @param response            Response object to be transformed.
    */
   void transformResponse(Map requestContext, Map response) {
      // set up delegate map
      def delegate = requestContext
      delegate.response = response.responseDetails

      // log response before transformation
      log.debug "Response before transformation: ${response.responseDetails}"

      // get onResponse handlers and call it
      def onResponseHandlers = transformers*.onResponse*.clone()
      onResponseHandlers*.delegate = delegate
      onResponseHandlers*.call()
      log.info "Finish transforming response"

      // log response before transformation
      log.debug "Response after transformation: ${response.responseDetails}"
   }

   private loadTransformers(scriptNames, initBindings) {
      def shell = new GroovyShell()
      def scripts = scriptNames.collect { shell.parse(it as File) }
      scripts.each { it.binding = new Binding(initBindings.clone()) }
      scripts*.run()
      return scripts*.binding
   }
}

