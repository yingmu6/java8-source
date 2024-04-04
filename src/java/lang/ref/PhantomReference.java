/*
 * Copyright (c) 1997, 2003, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.ref;


/**
 * Phantom reference objects, which are enqueued after the collector
 * determines that their referents may otherwise be reclaimed.  Phantom
 * references are most often used for scheduling pre-mortem cleanup actions in
 * a more flexible way than is possible with the Java finalization mechanism.
 *
 * <p> If the garbage collector determines at a certain point in time that the
 * referent of a phantom reference is <a
 * href="package-summary.html#reachability">phantom reachable</a>, then at that
 * time or at some later time it will enqueue the reference.
 *
 * <p> In order to ensure that a reclaimable object remains so, the referent of
 * a phantom reference may not be retrieved: The <code>get</code> method of a
 * phantom reference always returns <code>null</code>.
 *
 * <p> Unlike soft and weak references, phantom references are not
 * automatically cleared by the garbage collector as they are enqueued.  An
 * object that is reachable via phantom references will remain so until all
 * such references are cleared or themselves become unreachable.
 *
 * @author   Mark Reinhold
 * @since    1.2
 */

public class PhantomReference<T> extends Reference<T> { //虚引用Reference

    /**
     * PhantomReference 是所有“弱引用”中最弱的引用类型。不同于软引用和弱引用，虚引用无法通过
     * get() 方法来取得目标对象的强引用从而使用目标对象，从源码可以发现 get() 被重写为永远返回 null。
     *
     * 那虚引用到底有什么作用？其实虚引用主要被用来 跟踪对象被垃圾回收的状态，
     * 通过查看引用队列中是否包含对象所对应的虚引用来判断它是否 即将被垃圾回收，从而采取行动。
     *
     */

    /**
     * Returns this reference object's referent.  Because the referent of a
     * phantom reference is always inaccessible, this method always returns
     * <code>null</code>.
     *
     * @return  <code>null</code>
     */
    public T get() {
        return null;
    }

    /**
     * Creates a new phantom reference that refers to the given object and
     * is registered with the given queue.
     *
     * <p> It is possible to create a phantom reference with a <tt>null</tt>
     * queue, but such a reference is completely useless: Its <tt>get</tt>
     * method will always return null and, since it does not have a queue, it
     * will never be enqueued.
     *
     * @param referent the object the new phantom reference will refer to
     * @param q the queue with which the reference is to be registered,
     *          or <tt>null</tt> if registration is not required
     */
    public PhantomReference(T referent, ReferenceQueue<? super T> q) {
        super(referent, q);
    }

}
