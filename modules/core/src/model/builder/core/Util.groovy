package model.builder.core

import org.osgi.framework.BundleContext

class Util {

    static Expando cyReference(BundleContext bc, Closure cyAct, ... cyInterfaces) {
        Expando e = new Expando()
        cyInterfaces.each {
            def impl = cyAct.call(bc, it)
            def name = it.simpleName
            e.setProperty(name[0].toLowerCase() + name[1..-1], impl)
        }
        e
    }
}
