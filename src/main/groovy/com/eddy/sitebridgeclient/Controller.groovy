package com.eddy.sitebridgeclient

import org.apache.log4j.*
import java.util.concurrent.*

class Controller {
   private static Logger log = Logger.getLogger(this.name)
   private bridge
   private transformers
   private executor
   private done = false

   /**
    * Bridge Constructor. This is the full form constructor. Use this to initiate 
    * the bridge operation.
    * 
    * @param serverURL           URL to the bridge server.
    * @param endpointURL         URL to the endpoint site.
    * @param transformerScripts  List of transformer script file names.
    * @param reportOutputDir     Directory name for the report output.
    */
   Controller(String serverURL, String endpointURL, List<String> transformerScripts,
         String reportOutputDir) {
      // create all components needed
      bridge = new Bridge(serverURL, endpointURL)
      transformers = new Transformers(transformerScripts, serverURL, endpointURL)
      executor = Executors.newCachedThreadPool()
   }

   /**
    * Warmer constructor. This is a brief form constructor. Use this to initiate
    * the warming up process of the bridge server site.
    * 
    * @param serverURL           URL to the bridge server.
    */
   Controller(String serverURL) {
      bridge = new Bridge(serverURL, null)
      executor = Executors.newCachedThreadPool()
   }

   /**
    * Start the bridging process. This method returns immediately as it kicks off
    * the asynchronous process of bridging with the server. Call finish() to stop
    * the process.
    */
   void startBridging() {
      executor.submit( {
         try {
            // start with resetting the bridge
            bridge.reset()

            // now we loop until done
            while(!done) {
               // query for the requests. this return list of requests which could be empty
               def requests = bridge.query(); 

               // if requests found, satisfy them then immediately ask for more
               // if not we wait a little bit
               if (requests) {
                  // note that for each set of requests, we use a separate them to take care
                  // of them so that we don't have to wait until they are all done. This
                  // allows the main loop to go and fetch more request from the bridge
                  executor.submit( {
                     def futureResponses = requests.collect { request ->
                        // and now for each request, we also fetch the response asynchronousely.
                        // each thread will leave future so that the thread that's handling 
                        // the request set will be able to synchronize with all indepdently fetched
                        // request at the end
                        executor.submit({
                           try {
                              // here is the steps to process each request
                              def tctx = transformers.transformRequest(request)
                              def response = bridge.fetchResponse(request)
                              transformers.transformResponse(tctx, response)
                              return response
                           } catch(ex) { ex.printStackTrace(); log.error ex }
                        } as Callable<Object>)
                     }

                     // here is the sync point. we will wait until all requests are process
                     // then we satisfy all them at once. Note that some request processing
                     // might fail and in that case we will exclude it from the satisfaction
                     bridge.satisfy(futureResponses.collect { it.get() }.findAll { it != null })
                  } as Runnable)
               } else {
                  // wait a little bit if no request is found
                  Thread.currentThread().sleep(1000) 
               }
            }
         } catch(ex) { ex.printStackTrace(); log.error ex }
      } as Runnable)
   }

   /**
    * Start the warming up process. This method returns immediately as it kicks off
    * the asynchronous process of warming the server. Call finish() to stop
    * the process.
    */
   void startWarmingUp() {
      // this is how to perform it
      def warmup = { msg,times ->
         def queue = (1..times).collect {
            executor.submit({ bridge.warmup() } as Runnable)
         }
         queue.each { it.get() }
         log.info msg
      }

      // now the real thing
      executor.submit( {
         try {
            2.times { warmup("Kick off server #${it}", 40) }
            while(!done) {
               warmup("Keep server warm", 5)
            }
         } catch (ex) { log.error ex }
      } as Runnable)
   }

   /**
    * Finish this controller. Call this method to stop the controller after starting
    * either the bridging or warming up process.
    */
   void finish() {
      done = true
      executor.shutdown()
   }
}

