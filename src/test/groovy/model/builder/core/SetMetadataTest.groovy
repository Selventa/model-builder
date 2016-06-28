package model.builder.core

import org.cytoscape.equations.Interpreter
import org.cytoscape.equations.internal.interpreter.InterpreterImpl
import org.cytoscape.event.CyEventHelper
import org.cytoscape.model.CyEdge
import org.cytoscape.model.CyIdentifiable
import org.cytoscape.model.CyNetwork
import org.cytoscape.model.CyNetworkManager
import org.cytoscape.model.CyNetworkTableManager
import org.cytoscape.model.CyNode
import org.cytoscape.model.CyTable
import org.cytoscape.model.CyTableFactory
import org.cytoscape.model.internal.CyNetworkFactoryImpl
import org.cytoscape.model.internal.CyNetworkManagerImpl
import org.cytoscape.model.internal.CyNetworkTableManagerImpl
import org.cytoscape.model.internal.CyTableFactoryImpl
import org.cytoscape.model.internal.CyTableManagerImpl
import org.cytoscape.service.util.CyServiceRegistrar
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

import static model.builder.core.Util.setMetadata
import static org.junit.Assert.assertThat
import static org.hamcrest.Matchers.*

/**
 * Test setting Cytoscape metadata from a model network.
 *
 * Issue 541: http://redmine.selventa.com/issues/541
 */
class SetMetadataTest {

    @Mock private CyServiceRegistrar mockRegistrar
    private CyEventHelper cyEventHelper
    private CyTableManagerImpl cyTableMgr
    private CyNetworkManager cyNetworkMgr
    private CyNetworkTableManager cyNetworkTableMgr
    private CyTableFactory cyTableFct
    private CyNetworkFactoryImpl cyNetworkFct

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this)

        Interpreter intp = new InterpreterImpl();
        cyEventHelper = new DummyCyEventHelper(false);
        cyNetworkTableMgr = new CyNetworkTableManagerImpl();
        cyNetworkMgr = new CyNetworkManagerImpl(cyEventHelper);
        cyTableMgr = new CyTableManagerImpl(cyEventHelper, cyNetworkTableMgr, cyNetworkMgr);
        cyTableFct = new CyTableFactoryImpl(cyEventHelper, intp, mockRegistrar)
        cyNetworkFct = new CyNetworkFactoryImpl(cyEventHelper, cyTableMgr, cyNetworkTableMgr,
                cyTableFct, mockRegistrar);
    }

    @Test
    public void nullNetworkMetadata() {
        CyNetwork cyN = cyNetworkFct.createNetwork()
        def columns = cyN.defaultNetworkTable.columns
        setMetadata([null], [cyN], cyN.defaultNetworkTable)

        assertThat cyN.defaultNetworkTable.columns, equalTo(columns)
    }

    @Test
    public void canCallAsSingle() {
        CyNetwork cyN = cyNetworkFct.createNetwork()
        def columns = cyN.defaultNetworkTable.columns
        setMetadata(null, cyN, cyN.defaultNetworkTable)

        assertThat cyN.defaultNetworkTable.columns, equalTo(columns)
    }

    @Test(expected = NullPointerException.class)
    public void failNullPointerForNullCyIdentifiable() {
        CyNetwork cyN = cyNetworkFct.createNetwork()
        setMetadata([[:]], null, cyN.defaultNetworkTable)
    }

    @Test(expected = NullPointerException.class)
    public void failNullPointerForCyIdentifiableArrayContainingANull() {
        CyNetwork cyN = cyNetworkFct.createNetwork()
        setMetadata([[:]], [null], cyN.defaultNetworkTable)
    }

    @Test(expected = IllegalArgumentException.class)
    public void failIllegalArgumentWhenMetadataCyIdentifiableCardinalityDiffers() {
        CyNetwork cyN = cyNetworkFct.createNetwork()
        setMetadata([[:], [:]], [cyN], cyN.defaultNetworkTable)
    }

    @Test
    public void validMetadata() {
        CyNetwork cyN = cyNetworkFct.createNetwork()
        def metadata = [score: 5.0d, authors: 5, nick: "basic", "needs review": true]
        def metadataColumnCount = metadata.size()

        def testClosure = { List<Map> data, CyNetwork network, List<CyIdentifiable> cy, CyTable table ->
            def initial = table.columns.size()
            setMetadata(data, cy, table)
            // assert count of additional metadata columns
            assertThat table.columns.size(), equalTo(initial + metadataColumnCount)

            cy.each {
                // assert on score
                assertThat table.getColumn('score'), is(notNullValue())
                assertThat table.getColumn('score').type, equalTo(Double.class)
                assertThat cyN.getRow(it).get('score', Double.class), equalTo(5.0d)

                // assert on authors
                assertThat table.getColumn('authors'), is(notNullValue())
                assertThat table.getColumn('authors').type, equalTo(Integer.class)
                assertThat cyN.getRow(it).get('authors', Integer.class), equalTo(5)

                // assert on nick
                assertThat table.getColumn('nick'), is(notNullValue())
                assertThat table.getColumn('nick').type, equalTo(String.class)
                assertThat cyN.getRow(it).get('nick', String.class), equalTo("basic")

                // assert on 'needs review'
                assertThat table.getColumn('needs review'), is(notNullValue())
                assertThat table.getColumn('needs review').type, equalTo(Boolean.class)
                assertThat cyN.getRow(it).get('needs review', Boolean.class), equalTo(true)
            }
        }

        // test metadata on network
        testClosure.call([metadata], cyN, [cyN], cyN.defaultNetworkTable)

        // test metadata on nodes
        CyNode A = cyN.addNode()
        CyNode B = cyN.addNode()
        CyNode C = cyN.addNode()
        testClosure.call([metadata, metadata, metadata], cyN, [A, B, C], cyN.defaultNodeTable)

        // test metadata on edges
        CyEdge AB = cyN.addEdge(A, B, true)
        CyEdge BC = cyN.addEdge(B, C, true)
        testClosure.call([metadata, metadata], cyN, [AB, BC], cyN.defaultEdgeTable)
    }
}
