package model.builder.core.model.builder.core.rcr

import org.openbel.ws.api.WsAPI

import static java.util.Collections.emptySet

class EquivalenceIntersectOperation implements IntersectOperation<String, Set<String>> {

    private String knowledgeNetwork
    private WsAPI wsAPI

    public EquivalenceIntersectOperation(String knowledgeNetwork, WsAPI wsAPI) {
        if (!knowledgeNetwork) throw new NullPointerException("knowledgeNetwork is null")
        if (!wsAPI) throw new NullPointerException("wsAPI is null")
        this.knowledgeNetwork = knowledgeNetwork
        this.wsAPI = wsAPI
    }

    /**
     * {@inheritDoc}
     */
    @Override
    Set<String> intersect(Set<String> set1, Set<String> set2) {
        def nodes1 = wsAPI.resolveNodes(set1.asList(), knowledgeNetwork)
        if (!nodes1) return emptySet()
        def nodes2 = wsAPI.resolveNodes(set2.asList(), knowledgeNetwork)
        return (nodes1 as Set<String>).intersect((nodes2 as Set<String>))
    }
}
