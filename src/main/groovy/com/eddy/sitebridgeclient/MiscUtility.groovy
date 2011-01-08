package com.eddy.sitebridgeclient

import java.util.zip.*
import net.sf.json.*

/**
 * Provides utility methods that are used across in many places.
 */
class MiscUtility {

   /**
    * Compress the given object into a gzip byte array. This method works oppositely
    * with inflateByteArrayToObj method.
    *
    * @param obj                 An arbitary object.
    *
    * @return gzip byte array.
    */
   static deflateObjectToByteArray(obj) {
      def bytes = new ByteArrayOutputStream()
      def outstream = new ObjectOutputStream(new GZIPOutputStream(bytes))
      outstream.writeObject(obj)
      outstream.close()
      bytes.toByteArray()
   }

   /**
    * Decompress the given byte array into an object. This method works oppositely with
    * deflateObjectToByteArray method.
    *
    * @param bytearray           Gzip byte array.
    *
    * @return An object derived from the byte array.
    */
   static inflateByteArrayToObj(bytearray) {
      return new ObjectInputStream(
         new GZIPInputStream(new ByteArrayInputStream(bytearray))).readObject()

   }

   /**
    * Convert json object to actual map and array objects. 
    *
    * @param jsonObjt            A JSONObject object.
    *
    * @return Map or List object structurally representing the same object.
    */
   static convertToMapAndArray(jsonObj) {
     switch(jsonObj) {
        case List: 
           return jsonObj.inject([]) { l, elem -> l << convertToMapAndArray(elem); l }
        case Map:  
           return jsonObj.inject([:]) { m, entry -> m[entry.key] = convertToMapAndArray(entry.value); m }
        case JSONNull:
           return null
        default:   
           return jsonObj
     }
   }

   /**
    * Convert the given list of integer into a byte array. 
    *
    * @param list                Any type of list object containing integers.
    *
    * @return byte[] object.
    */
   static convertIntegerListToByteArray(List list) {
      def bytearray = new byte[list.size()]
      list.eachWithIndex { v,i -> bytearray[i] = v }
      return bytearray
   }

   /**
    * Utility method helping parsing the hostname in RFC2732 format.
    *
    * @param rfc2732             Host name inf RFC 2732 format.
    * 
    * @return hostname which is either extracted portion or the origina value
    *    if parsing fail.
    */
   static String parseHostnameRFC2732Format(String rfc2732) {
      // extract only the host name and port. Either return extract
      // portion or the original value
      def match = (rfc2732 =~ /values:\[([^,]+),([^\]]*)]/)
      return match ? "${match[0][1]}${match[0][2]}" : rfc2732
   }
}

