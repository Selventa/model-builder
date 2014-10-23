package model.builder.core.model.builder.core.rcr

/**
 * Defines an intersection operation between two {@link Collection collections}
 * of type {@code <T>}.
 *
 * @param <T>; type of element contained in {@link Collection collections}
 * {@code c1} and {@code c2}
 * @param <C>; {@link Collection Collection&lt;T&gt;}
 */
public interface IntersectOperation<T, C extends Collection<T>> {

    /**
     * Intersects {@link Collection Collection&lt;T&gt;} {@code c1} with
     * {@link Collection Collection&lt;T&gt;} {@code c2}; returns the shared
     * elements as a {@link Collection Collection&lt;T&gt;}.
     *
     * @param c1; {@link Collection Collection&lt;T&gt;}
     * @param c2; {@link Collection Collection&lt;T&gt;}
     * @return {@link Collection Collection&lt;T&gt;} of shared elements; will not be
     * {@code null} but may be an empty {@link Collection Collection&lt;T&gt;} (e.g. Ø).
     */
    public C intersect(C c1, C c2);
}