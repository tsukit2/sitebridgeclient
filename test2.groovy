@Grab(group='net.sf.json-lib', module='json-lib', version='2.3', classifier='jdk15' )

import net.sf.json.*
import net.sf.json.groovy.*;


def m = [d:new Date()]
def j = JSONObject.fromObject(m)
def s = j.toString()
println s
def m2 = JSONObject.fromObject(s)
println m2.toString()
println m2.getDate(



def a = [] //[1,2,3,4,5,6,7]
println a[-0..-0]
