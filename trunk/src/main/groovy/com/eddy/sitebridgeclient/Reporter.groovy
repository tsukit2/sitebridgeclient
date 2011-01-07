package com.eddy.sitebridgeclient

import net.sf.json.*
import groovy.text.*
import groovy.xml.MarkupBuilder 

/**
 * Provide reporting service. This class handles the report creation and generation. 
 * It can be used to dump out the data to report. It can also be used to read in the
 * dumped data in order to generate an HTML report.
 */
class Reporter {

   private baseDir

   /**
    * Constructor.
    *
    * @param reportDir           Location where report files will be created.
    */
   Reporter(String reportDir) {
      // create base director if it doesn't exist
      this.baseDir = new File(reportDir)
      if (!baseDir.exists() && !baseDir.mkdirs()) {
         throw new IOException("Cannot create report directory: ${reportDir}")
      }
   }

   /**
    * Start a new report. Call this method to create a new report object where
    * you can use along with other report* methods.
    *
    * @return a Map representing a report.
    */
   Map startNewReport() {
      return new JSONObject([data:new JSONObject(), id:System.nanoTime(), timestamp:System.currentTimeMillis()])
   }

   /**
    * Report the original request.
    *
    * @param report           Report object obtained from startNewReport().
    * @param request          Request object to be reported.
    */
   void reportOriginalRequest(Map report, Map request) {
      report.data.originalRequest = JSONObject.fromObject(request)
   }

   /**
    * Report the final request.
    *
    * @param report           Report object obtained from startNewReport().
    * @param request          Request object to be reported.
    */
   void reportFinalRequest(Map report, Map request) {
      report.data.finalRequest = JSONObject.fromObject(request)
   }

   /**
    * Report the original response.
    *
    * @param report           Report object obtained from startNewReport().
    * @param response         Resonse object to be reported.
    */
   void reportOriginalResponse(Map report, Map response) {
      report.data.originalResponse = JSONObject.fromObject(response)
   }

   /**
    * Report the final response.
    *
    * @param report           Report object obtained from startNewReport().
    * @param response         Resonse object to be reported.
    */
   void reportFinalResponse(Map report, Map response) {
      report.data.finalResponse = JSONObject.fromObject(response)
   }
   
   /**
    * Finish the report. Call this to write off the report to a file that can be 
    * read later.
    *
    * @param report           Report object obtained from startNewReport().
    */
   void finishReport(Map report) {
      def reportFile = new File(baseDir, "report-${report.id}.json")
      reportFile.setText(report.toString(3), 'UTF8')
   }

   void generateReport(Integer limit) {
      // loads up all the files
      def allfiles = []
      baseDir.eachFileMatch(~/.*\.json$/) { allfiles << it }
      allfiles.sort()

      // limit the result if necessary
      if (limit != null) {
         allfiles = allfiles ? allfiles[-(Math.min(limit,allfiles.size()))..-1] : allfiles
      }

      // if there is still files left, generate the files
      if (allfiles) {
         generateHTMLReport(allfiles)
      }
   }

   private generateHTMLReport(allfiles) {
      // create html folder for output files
      def htmlFolder = new File(baseDir, 'html')
      if(!htmlFolder.exists() && !htmlFolder.mkdir()) {
         throw new IOException("Cannot create report folder: ${htmlFolder}")
      }

      // create the report
      new File(htmlFolder, 'index.html').newPrintWriter('utf8').withPrintWriter { writer ->
         def builder = new MarkupBuilder(writer)
         builder.html {
            head {
               title "Site Bridge Report"
            }
            body {
               h1 "Sitebridge Report"
               h3 "Generated on ${new Date()}"
               hr()
               table(border:1) {
                  tr {
                     th '#'
                     th 'Timestamp'
                     th 'Method'
                     th 'Path'
                     th 'Status'
                     th 'Bridged Request'
                     th 'Bridged Response'
                     th 'Browser Request'
                     th 'Browser Response'
                  }
                  allfiles.eachWithIndex { file, index ->
                     def report = MiscUtility.convertToMapAndArray(JSONObject.fromObject(file.text))
                     report.timestamp = new Date(report.timestamp) // convert from long to date
                     tr {
                        def path = report.data.finalRequest.requestDetails.pathInfo ?: '/'
                        td index + 1
                        td report.timestamp
                        td report.data.finalRequest.requestDetails.method
                        td path
                        td report.data.originalResponse.responseDetails.status
                        td { 
                           a(href:createRequestPage("Bridge Request: ${path}", path, htmlFolder, report, 'finalRequest'),
                             target:'_blank') {
                              builder.yield 'details'
                           }
                        }
                        td { 
                           a(href:createResponsePage("Bridge Response: ${path}", htmlFolder, report, 'originalResponse'),
                             target:'_blank') {
                              builder.yield 'details'
                           }
                        }
                        td { 
                           a(href:createRequestPage("Browser Request: ${path}", path, htmlFolder, report, 'originalRequest'),
                             target:'_blank') {
                              builder.yield 'details'
                           }
                        }
                        td { 
                           a(href:createResponsePage("Browser Response: ${path}", htmlFolder, report, 'finalResponse'),
                             target:'_blank') {
                              builder.yield 'details'
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private createRequestPage(pageTitle, path, htmlFolder, report, what) {
      // create the report
      def request = report.data[what].requestDetails
      def file = new File(htmlFolder, "${report.id}-${what}.html")
      file.newPrintWriter('utf8').withPrintWriter { writer ->
         def builder = new MarkupBuilder(writer)
         builder.html {
            head {
               title "Site Bridge Report - ${pageTitle}"
            }
            body {
               h1 pageTitle
               hr()
               table(border:1) {
                  tr {
                     th 'What'
                     th 'Data'
                  }
                  tr {
                     td 'Timestamp'
                     td report.timestamp
                  }
                  tr {
                     td 'Method'
                     td request.method
                  }
                  tr {
                     td 'Path'
                     td path
                  }
                  tr {
                     td 'URL Params'
                     td {
                        request.query?.each { q ->
                           p "${q.key}: ${q.value}"
                        }
                        mkp.yieldUnescaped '&nbsp;'
                     }
                  } 
                  tr {
                     td 'Body Params'
                     td {
                        request.params?.each { pr ->
                           p "${pr.key}: ${pr.value}"
                        }
                        mkp.yieldUnescaped '&nbsp;'
                     }
                  }
                  tr {
                     td 'Headers'
                     td {
                        request.headers?.each { h ->
                           if (h.value != null && h.value instanceof List) {
                              h.value.each { v ->
                                 p "${h.key}: ${v}"
                              }
                           } else {
                              p "${h.key}: ${h.value}"
                           }
                        }
                        mkp.yieldUnescaped '&nbsp;'
                     }
                  }
                  tr {
                     td 'Body'
                     td {
                        if (request.bodyBytes) {
                           def bodyfile = new File(htmlFolder, "${report.id}-${what}-body.raw")
                           bodyfile.delete()
                           bodyfile << MiscUtility.convertIntegerListToByteArray(request.bodyBytes)
                           a(href:bodyfile.toURL(), type:request.headers.'Content-Type', target:'_blank') {
                              builder.yield 'content'
                           }
                        } else {
                           mkp.yieldUnescaped '&nbsp;'
                        }
                     }
                  }
               }
            }
         }
      }

      // return the url for this file
      return file.toURL()
   }

   private createResponsePage(pageTitle, htmlFolder, report, what) {
      // create the report
      def response = report.data[what].responseDetails
      def file = new File(htmlFolder, "${report.id}-${what}.html")
      file.newPrintWriter('utf8').withPrintWriter { writer ->
         def builder = new MarkupBuilder(writer)
         builder.html {
            head {
               title "Site Bridge Report - ${pageTitle}"
            }
            body {
               h1 pageTitle
               hr()
               table(border:1) {
                  tr {
                     th 'What'
                     th 'Data'
                  }
                  tr {
                     td 'Timestamp'
                     td report.timestamp
                  }
                  tr {
                     td 'Status'
                     td response.status
                  }
                  tr {
                     td 'Headers'
                     td {
                        response.headers?.each { h ->
                           if (h.value != null && h.value instanceof List) {
                              h.value.each { v ->
                                 p "${h.key}: ${v}"
                              }
                           } else {
                              p "${h.key}: ${h.value}"
                           }
                        }
                        mkp.yieldUnescaped '&nbsp;'
                     }
                  }
                  tr {
                     td 'Body'
                     td {
                        if (response.bodyBytes) {
                           def bodyfile = new File(htmlFolder, "${report.id}-${what}-body.raw")
                           bodyfile.delete()
                           bodyfile << MiscUtility.convertIntegerListToByteArray(response.bodyBytes)
                           a(href:bodyfile.toURL(), type:response.headers.'Content-Type', target:'_blank') {
                              builder.yield 'content'
                           }
                        } else {
                           mkp.yieldUnescaped '&nbsp;'
                        }
                     }
                  }
               }
            }
         }
      }

      // return the url for this file
      return file.toURL()
   }
}

