package com.eddy.sitebridgeclient

import net.sf.json.*
import groovy.text.*

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
      if (!baseDir.exists()) {
         if (!baseDir.mkdirs()) {
            throw new IOException("Cannot create report directory: ${reportDir}")
         }
      }
   }

   /**
    * Start a new report. Call this method to create a new report object where
    * you can use along with other report* methods.
    *
    * @return a Map representing a report.
    */
   Map startNewReport() {
      return [data:new JSONObject(), id:System.nanoTime()]
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
      reportFile.setText(report.data.toString(3), 'UTF8')
   }

   void generateReport(Integer limit) {
      // loads up all the files
      def allfiles = []
      baseDir.eachFileMatch(~/.*\.json$/) { allfiles << it }
      allfiles.sort({ a,b -> b.compareTo(a) })

      // limit the result if necessary
      if (limit != null) {
         allfiles = allfiles[0..<limit]
      }

      // if there is still files left, generate the files
      if (allfiles) {
         generateHTMLReport(allfiles)
      }
   }

   private generateHTMLReport(allfiles) {
      // create html folder for output files
      def htmlFolder = new File(baseDir, 'html')
      if(!htmlFolder.mkdir()) {
         throw new IOException("Cannot create folder: ${htmlFolder}")
      }

      // define template to use
      def template = new SimpleTemplateEngine().createTemplate(
'''
<html>
   <title>SiteBridge Report</title>
   <body>
      <h1>SiteBridge Report<h1>
      <h3>Generated on ${new Date()}</h3>
      <br/>

      <table>
         <th>
            <td>Timestamp</td>
            <td>Path</td>
            <td>Status<td>
            <td>Request</td>
            <td>Response</td>
            <td>Unbridged Response</td>
            <td>Unbridged Response</td>
         </th>
         <% allfiles.each { file -> %>
            <%=generateHTMLReportRow(htmlFolder, file)%>
         <% } %>
      </table>
   </body>
</html>
'''

      // now generate the report
      new File(htmlFolder, 'index.html').text = 
         template.make([
            htmlFolder:htmlFolder,
            allfiles:allfiles,
            generateHTMLReportRow:this.&generateHTMLReportRow]).toString()
   }

   private generateHTMLReportRow(htmlFolder, file) {


   }
}

