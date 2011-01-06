@Grab(group='net.sf.json-lib', module='json-lib', version='2.3', classifier='jdk15' )

import net.sf.json.*
import net.sf.json.groovy.*;


def j = new JSONObject([a:1, b:2])
println j.toString()

println new File('abc').toURI()

new URL('file:///C:/Personal/development/gaelyk/sitebridgeclient/abc')



