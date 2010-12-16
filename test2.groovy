@Grab(group='net.sf.json-lib', module='json-lib', version='2.3', classifier='jdk15' )

import net.sf.json.*
import net.sf.json.groovy.*;

class Person {
   String name
   int age
}

def jsonObj = JSONObject.fromObject('''{"responseIndex":1,"responseDetails":{"responseText":"Current Time: Thu Dec 16 03:57:39 PST 2010"}}''')
convert(jsonObj)


def convert(jsonObj) {
    switch(jsonObj) {
        case List:
            return jsonObj.inject([]) { l, elem -> l << convert(elem) }
        case Map:
            return jsonObj.inject([:]) { m, entry -> m[entry.key] = convert(entry.value) }
        default:
           return jsonObj
    }
}
    

//JSONSerializer.toJava(JSONObject.fromObject('''{"responseIndex":1,"responseDetails":{"responseText":"Current Time: Thu Dec 16 03:57:39 PST 2010"}}'''))
//JSONObject.toBean(JSONObject.fromObject('''{"responseIndex":1,"responseDetails":{"responseText":"Current Time: Thu Dec 16 03:57:39 PST 2010"}}'''), Map)

//println (new Person(name:'Ed', age:65) as JSONObject)

//println JSONObject.fromObject(new Person(name:'Ed', age:65)).toString() 
//println JSONArray.fromObject([[name:'Ed', age:65, aa:null]]).toString()
//println JSONArray.fromObject([[name:'Ed', age:65, aa:null]]).aa.isNull()
//println JSONObject.fromObject(null).isNullObject()
//println JSONObject.fromObject(null).toString()
//println JSONObject.fromObject(JSONObject.fromObject(null).toString()).toString()
//println JSONObject.toBean(JSONObject.fromObject(new Person(name:'Ed', age:65)), Map)
//println JSONObject.fromObject(true)
//println JSONObject.fromObject('')

//JSONObject.methods.each { println it }
