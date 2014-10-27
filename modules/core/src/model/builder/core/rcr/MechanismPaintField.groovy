package model.builder.core.rcr

import org.cytoscape.view.vizmap.mappings.BoundaryRangeValues
import org.cytoscape.view.vizmap.mappings.ContinuousMapping
import org.cytoscape.view.vizmap.mappings.DiscreteMapping

import java.awt.Color
import java.awt.Paint

import static model.builder.core.Activator.CY
import static org.cytoscape.view.presentation.property.BasicVisualLexicon.NODE_FILL_COLOR
import static org.cytoscape.view.presentation.property.BasicVisualLexicon.NODE_PAINT

public enum MechanismPaintField {
    DIRECTION  ("direction",   String.class),
    CONCORDANCE("concordance", Double.class),
    RICHNESS   ("richness",    Double.class);

    String field
    Class<?> type

    MechanismPaintField(String field, Class<?> type) {
        this.field = field
        this.type  = type
    }

    static MechanismPaintField fromField(String field) {
        values().find { field == it.field }
    }

    String toString() {
        this.field.capitalize()
    }

    static DiscreteMapping createDirectionFunction() {
        DiscreteMapping fx = CY.discreteMapping.createVisualMappingFunction(
                DIRECTION.field, DIRECTION.type, NODE_FILL_COLOR) as DiscreteMapping
        fx.putMapValue(null,   new Color(0xAAAAAA))
        fx.putMapValue('',     new Color(0xAAAAAA))
        fx.putMapValue('None', new Color(0xFFFFFF))
        fx.putMapValue('None', new Color(0xFFFFFF))
        fx.putMapValue('Down', new Color(0x000666))
        fx.putMapValue('Up',   new Color(0xFFA000))
        fx
    }

    static ContinuousMapping createConcordanceFunction() {
        withRange(CY.continuousMapping.createVisualMappingFunction(
                CONCORDANCE.field, CONCORDANCE.type, NODE_PAINT) as ContinuousMapping)
    }

    static ContinuousMapping createRichnessFunction() {
        withRange(CY.continuousMapping.createVisualMappingFunction(
                RICHNESS.field, RICHNESS.type, NODE_PAINT) as ContinuousMapping)
    }

    private static ContinuousMapping withRange(ContinuousMapping fx) {
        Double v1 = 0.001
        BoundaryRangeValues<Paint> r1 = new BoundaryRangeValues<Paint>(
                new Color(0x000666), new Color(0x000666), new Color(0x000666)
        )
        fx.addPoint(v1, r1)

        Double v2 = 0.005
        BoundaryRangeValues<Paint> r2 = new BoundaryRangeValues<Paint>(
                new Color(0x333399), new Color(0x333399), new Color(0x333399)
        )
        fx.addPoint(v2, r2)

        Double v3 = 0.01
        BoundaryRangeValues<Paint> r3 = new BoundaryRangeValues<Paint>(
                new Color(0x6666CC), new Color(0x6666CC), new Color(0x6666CC)
        )
        fx.addPoint(v3, r3)

        Double v4 = 0.05
        BoundaryRangeValues<Paint> r4 = new BoundaryRangeValues<Paint>(
                new Color(0x9999FF), new Color(0x9999FF), new Color(0x9999FF)
        )
        fx.addPoint(v4, r4)

        Double v5 = 0.1
        BoundaryRangeValues<Paint> r5 = new BoundaryRangeValues<Paint>(
                new Color(0xCCCCFF), new Color(0xCCCCFF), new Color(0xCCCCFF)
        )
        fx.addPoint(v5, r5)

        Double v6 = 1.0
        BoundaryRangeValues<Paint> r6 = new BoundaryRangeValues<Paint>(
                new Color(0xFFFFFF), new Color(0xFFFFFF), new Color(0xFFFFFF)
        )
        fx.addPoint(v6, r6)
        fx
    }

    /*

            case 'Down':
                def down = [(0.0..0.001):  '#000666', (0.001..0.005): '#333399',
                            (0.005..0.01): '#6666CC', (0.01..0.05):   '#9999FF',
                            (0.05..0.1):   '#CCCCFF', (0.1..1):       '#FFFFFF']
                return down.find {it.key.containsWithinBounds(concordance)}?.value
            case 'Up':
                def up = [(0.0..0.001):  '#FFA000', (0.001..0.005): '#FFC800',
                          (0.005..0.01): '#FFE800', (0.01..0.05):   '#FFF800',
                          (0.05..0.1):   '#FFFF99', (0.1..1):       '#FFFFFF']
                return up.find {it.key.containsWithinBounds(concordance)}?.value
            case 'None':
                return '#FFFFFF'
            default:
                return '#AAAAAA'

     */
}
