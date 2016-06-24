package model.builder.core

import org.junit.Test

import static org.junit.Assert.assertThat
import static org.hamcrest.Matchers.*

/**
 * Tests {@link Util#classReduce(java.util.List)}.
 */
class MetadataTest {

    @Test
    public void oneTypeYieldItself() {
        assertThat Util.classReduce([null, 3]), equalTo(Integer.class)
        assertThat Util.classReduce([null, 3.000D]), equalTo(Double.class)
        assertThat Util.classReduce([null, "3"]), equalTo(String.class)
        assertThat Util.classReduce([null, true]), equalTo(Boolean.class)
        assertThat Util.classReduce([null, 3.0F]), equalTo(Float.class)
        assertThat Util.classReduce([null, ["foo", "bar"]]), typeCompatibleWith(List.class)
    }

    @Test
    public void nullYieldString() {
        assertThat Util.classReduce([null, null]), equalTo(String.class)
        assertThat Util.classReduce(['null', 'null']), equalTo(String.class)
    }

    @Test
    public void multiTypedYieldString() {
        assertThat Util.classReduce(["true", false]), equalTo(String.class)
        assertThat Util.classReduce([true, false, 1.0]), equalTo(String.class)
        assertThat Util.classReduce([1, "foo", null]), equalTo(String.class)
        assertThat Util.classReduce([5.0D, 0, 3.0F]), equalTo(String.class)
    }

    @Test
    public void integerTypeToDouble() {
        assertThat Util.classReduce([1, 5.0D, 100]), equalTo(Double.class)
        assertThat Util.classReduce([10.0D, 100, 1000]), equalTo(Double.class)
        assertThat Util.classReduce([null, 1, null, 100.0D]), equalTo(Double.class)
    }
}
