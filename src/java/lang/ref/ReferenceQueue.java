/*
 * Copyright (c) 1997, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.util.function.Consumer;

/**
 * Reference queues, to which registered reference objects are appended by the
 * garbage collector after the appropriate reachability changes are detected.
 *
 * @author   Mark Reinhold
 * @since    1.2
 */

public class ReferenceQueue<T> {
    /**
     * @csy-006-P3 引用队列待了解？
     * 解：引用队列，在检测到适当的可到达性更改后，垃圾回收器将已注册的引用对象添加到该队列中
     *
     * ReferenceQueue名义上是一个队列，但实际内部并非有实际的存储结构，它的存储是依赖于内部节点之间的关系来表达。
     * 可以理解为queue是一个类似于链表的结构，这里的节点其实就是reference本身。可以理解为queue为一个链表的容器，
     * 其自己仅存储当前的head节点，而后面的节点由每个reference节点自己通过next来保持即可。
     *
     * Reference与ReferenceQueue详解  https://www.jianshu.com/p/f86d3a43eec5
     *
     *
     * ReferenceQueue是用来保存需要关注的Reference队列
     * ReferenceQueue内部实现实际上是一个栈
     * ReferenceQueue可以用来进行数据监控，资源释放等
     *
     * Java引用类型之——ReferenceQueue源码详解 https://cloud.tencent.com/developer/article/1377457
     */

    /**
     * Constructs a new reference-object queue.
     */
    public ReferenceQueue() { }

    /**
     * @csy-006-P3 Null对象是怎么使用的？
     * 解：用来做为特殊标记的静态成员变量，只是简单继承了ReferenceQueue的一个类，
     * 为什么不直接new一个ReferenceQueue呢？这里自然是有它的道理的，如果直接使用ReferenceQueue，
     * 就会导致有可能误操作这个NULL和ENQUEUED变量，因为ReferenceQueue中enqueue方法是需要使用lock对象锁的，
     * 这里覆盖了这个方法并直接返回false，这样就避免了乱用的可能性，也避免了不必要的资源浪费。
     */
    private static class Null<S> extends ReferenceQueue<S> {
        boolean enqueue(Reference<? extends S> r) {
            return false;
        }
    }

    static ReferenceQueue<Object> NULL = new Null<>();
    static ReferenceQueue<Object> ENQUEUED = new Null<>();

    static private class Lock { };
    private Lock lock = new Lock();
    private volatile Reference<? extends T> head = null;
    private long queueLength = 0;

    boolean enqueue(Reference<? extends T> r) { /* Called only by Reference class */
        synchronized (lock) {
            // Check that since getting the lock this reference hasn't already been
            // enqueued (and even then removed)
            ReferenceQueue<?> queue = r.queue;
            if ((queue == NULL) || (queue == ENQUEUED)) {
                return false;
            }

            /**
             * @csy-006-P3 assert 的功能用途是怎样？
             * 解：assert关键字语法，有两种用法：
             *   1、assert <boolean表达式>
             *      如果<boolean表达式>为true，则程序继续执行。
             *      如果为false，则程序抛出AssertionError，并终止执行。
             *
             *   2、assert <boolean表达式> : <错误信息表达式>
             *      如果<boolean表达式>为true，则程序继续执行。
             *      如果为false，则程序抛出java.lang.AssertionError，并输入<错误信息表达式>。
             */
            assert queue == this;
            r.queue = ENQUEUED;
            r.next = (head == null) ? r : head;
            head = r;
            queueLength++;
            if (r instanceof FinalReference) {
                sun.misc.VM.addFinalRefCount(1);
            }
            lock.notifyAll();
            return true;
        }
    }

    private Reference<? extends T> reallyPoll() {       /* Must hold lock */
        Reference<? extends T> r = head;
        if (r != null) {
            @SuppressWarnings("unchecked")
            Reference<? extends T> rn = r.next;
            head = (rn == r) ? null : rn;
            r.queue = NULL;
            r.next = r;
            queueLength--;
            if (r instanceof FinalReference) {
                sun.misc.VM.addFinalRefCount(-1);
            }
            return r;
        }
        return null;
    }

    /**
     * Polls this queue to see if a reference object is available.  If one is
     * available without further delay then it is removed from the queue and
     * returned.  Otherwise this method immediately returns <tt>null</tt>.
     *
     * @return  A reference object, if one was immediately available,
     *          otherwise <code>null</code>
     */
    public Reference<? extends T> poll() {
        if (head == null)
            return null;
        synchronized (lock) {
            return reallyPoll();
        }
    }

    /**
     * Removes the next reference object in this queue, blocking until either
     * one becomes available or the given timeout period expires.
     *
     * <p> This method does not offer real-time guarantees: It schedules the
     * timeout as if by invoking the {@link Object#wait(long)} method.
     *
     * @param  timeout  If positive, block for up to <code>timeout</code>
     *                  milliseconds while waiting for a reference to be
     *                  added to this queue.  If zero, block indefinitely.
     *
     * @return  A reference object, if one was available within the specified
     *          timeout period, otherwise <code>null</code>
     *
     * @throws  IllegalArgumentException
     *          If the value of the timeout argument is negative
     *
     * @throws  InterruptedException
     *          If the timeout wait is interrupted
     */
    public Reference<? extends T> remove(long timeout)
        throws IllegalArgumentException, InterruptedException
    {
        if (timeout < 0) {
            throw new IllegalArgumentException("Negative timeout value");
        }
        synchronized (lock) {
            Reference<? extends T> r = reallyPoll();
            if (r != null) return r;
            long start = (timeout == 0) ? 0 : System.nanoTime();
            for (;;) {
                lock.wait(timeout);
                r = reallyPoll();
                if (r != null) return r;
                if (timeout != 0) {
                    long end = System.nanoTime();
                    timeout -= (end - start) / 1000_000;
                    if (timeout <= 0) return null;
                    start = end;
                }
            }
        }
    }

    /**
     * Removes the next reference object in this queue, blocking until one
     * becomes available.
     *
     * @return A reference object, blocking until one becomes available
     * @throws  InterruptedException  If the wait is interrupted
     */
    public Reference<? extends T> remove() throws InterruptedException {
        return remove(0);
    }

    /**
     * Iterate queue and invoke given action with each Reference.
     * Suitable for diagnostic purposes.
     * WARNING: any use of this method should make sure to not
     * retain the referents of iterated references (in case of
     * FinalReference(s)) so that their life is not prolonged more
     * than necessary.
     */
    void forEach(Consumer<? super Reference<? extends T>> action) {
        for (Reference<? extends T> r = head; r != null;) {
            action.accept(r);
            @SuppressWarnings("unchecked")
            Reference<? extends T> rn = r.next;
            if (rn == r) {
                if (r.queue == ENQUEUED) {
                    // still enqueued -> we reached end of chain
                    r = null;
                } else {
                    // already dequeued: r.queue == NULL; ->
                    // restart from head when overtaken by queue poller(s)
                    r = head;
                }
            } else {
                // next in chain
                r = rn;
            }
        }
    }
}
