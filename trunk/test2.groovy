@Grab(group='net.sf.json-lib', module='json-lib', version='2.3', classifier='jdk15' )

import net.sf.json.*
import net.sf.json.groovy.*;


def data = [a:1, b:[2,4,5,6] as byte[]]
def jsonstr = JSONObject.fromObject(data).toString()
def data2 = JSONObject.fromObject(jsonstr)

println jsonstr
println data2