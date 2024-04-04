/*
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

/*
 *
 *
 *
 *
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent;

/**
 * A mix-in style（混合风格） interface for marking objects that should be
 * acted upon（此后） after a given delay.
 *
 * <p>An implementation of this interface must define a
 * {@code compareTo} method that provides an ordering consistent with
 * its {@code getDelay} method.（实现类需要实现compareTo方法）
 *
 * @since 1.5
 * @author Doug Lea
 */
public interface Delayed extends Comparable<Delayed> {

     /**
     * Returns the remaining delay associated with this object, in the
     * given time unit.
     *
     * @param unit the time unit
     * @return the remaining delay; zero or negative values indicate
     * that the delay has already elapsed（过期了）
     */
    long getDelay(TimeUnit unit); //
}
