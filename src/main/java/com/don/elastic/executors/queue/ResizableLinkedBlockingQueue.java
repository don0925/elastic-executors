package com.don.elastic.executors.queue;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * 参考LinkedBlockingQueue实现可变容量的阻塞队列
 * @param <E>
 * @author don du
 */
public class ResizableLinkedBlockingQueue<E> extends AbstractQueue<E> implements ResizableBlockingQueue<E>, Serializable {

    /**
     * 链表节点类定义
     * @param <E>
     */
    static class Node<E> {
        E item;

        Node<E> next;

        Node(E x) {
            item = x;
        }
    }

    /**
     * 可修改的容量，默认最大Integer.MAX_VALUE
     */
    private volatile int capacity;

    /**
     * 当前元素个数
     */
    private final AtomicInteger count = new AtomicInteger();

    /**
     * 链表表头，head.item = null
     */
    transient Node<E> head;

    /**
     * 链表表尾，last.next == null
     */
    private transient Node<E> last;

    /**
     * task, pool操作锁
     */
    private final ReentrantLock takeLock = new ReentrantLock();

    /**
     * take操作等待条件
     */
    private final Condition notEmpty = takeLock.newCondition();

    /**
     * put, offer操作锁
     */
    private final ReentrantLock putLock = new ReentrantLock();

    /**
     * put操作等待条件
     */
    private final Condition notFull = putLock.newCondition();

    /**
     * 为等待的take操作释放信号
     */
    private void signalNotEmpty() {
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lock();
        try {
            notEmpty.signal();
        } finally {
            takeLock.unlock();
        }
    }

    /**
     * 为等待的put操作释放信号
     */
    private void signalNotFull() {
        final ReentrantLock putLock = this.putLock;
        putLock.lock();
        try {
            notFull.signal();
        } finally {
            putLock.unlock();
        }
    }

    /**
     * 节点入队
     * @param node
     */
    private void enqueue(Node<E> node) {
        last = last.next = node;
    }

    /**
     * 节点出队
     */
    private E dequeue() {
        Node<E> h = head;
        Node<E> first = head.next;
        h.next = h; // 帮助GC
        head = first;
        E x = first.item;
        first.item = null;
        return x;
    }

    /**
     * 上锁阻止take和put操作
     */
    void fullyLock() {
        takeLock.lock();
        putLock.lock();
    }

    /**
     * 释放锁允许take和put操作
     */
    void fullyUnlock() {
        takeLock.unlock();
        putLock.unlock();
    }

    /**
     * 使用Integer.MAX_VALUE容量创建队列
     */
    public ResizableLinkedBlockingQueue() {
        this(Integer.MAX_VALUE);
    }

    /**
     * 使用指定容量创建队列
     * @param capacity
     */
    public ResizableLinkedBlockingQueue(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException();
        }

        this.capacity = capacity;
        last = head = new Node<>(null);
    }

    /**
     * 使用Integer.MAX_VALUE容量创建队列，并按照遍历顺序添加指定的集合元素到队列中
     * @param collection
     */
    public ResizableLinkedBlockingQueue(Collection<? extends E> collection) {
        this(Integer.MAX_VALUE);

        final ReentrantLock putLock = this.putLock;
        putLock.lock();
        try {
            int n = 0;
            for(E e : collection) {
                if (e == null) {
                    throw new NullPointerException();
                }
                if (n == capacity) {
                    throw new IllegalStateException("Queue full");
                }
                enqueue(new Node<>(e));
                n++;
            }
            count.set(n);
        } finally {
            putLock.unlock();
        }
    }

    /**
     * 修改队列大小，线程安全的
     * @param capacity
     */
    @Override
    public void setCapacity(int capacity) {
        fullyLock();
        try {
            this.capacity = capacity;
        } finally {
            fullyUnlock();
        }
    }


    @Override
    public int size() {
        return count.get();
    }

    @Override
    public int remainingCapacity() {
        // 可能会出现负值
        return capacity - count.get();
    }

    @Override
    public void put(E e) throws InterruptedException {
        if (e == null) {
            throw new NullPointerException();
        }

        int c = -1;
        final ReentrantLock putLock = this.putLock;
        final AtomicInteger count = this.count;
        putLock.lockInterruptibly();
        try {
            while (count.get() == capacity) {
                notFull.await();
            }
            enqueue(new Node<>(e));
            c = count.getAndIncrement();
            if (c + 1 < capacity) {
                notFull.signal();
            }
        } finally {
            putLock.unlock();
        }

        if (c == 0) {
            signalNotEmpty();
        }
    }

    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        if (e == null) {
            throw new NullPointerException();
        }
        long nanos = unit.toNanos(timeout);
        int c = -1;
        final ReentrantLock putLock = this.putLock;
        final AtomicInteger count = this.count;
        putLock.lockInterruptibly();
        try {
            while (count.get() == capacity) {
                if (nanos <= 0) {
                    return false;
                }
                nanos = notFull.awaitNanos(nanos);
            }
            enqueue(new Node<>(e));
            c = count.getAndIncrement();
            if (c + 1 < capacity) {
                notFull.signal();
            }
        } finally {
            putLock.unlock();
        }
        if (c == 0) {
            signalNotEmpty();
        }
        return true;
    }

    @Override
    public boolean offer(E e) {
        if (e == null) {
            throw new NullPointerException();
        }
        final AtomicInteger count = this.count;
        if (count.get() == capacity) {
            return false;
        }
        int c = -1;
        final ReentrantLock putLock = this.putLock;
        putLock.lock();
        try {
            if (count.get() < capacity) {
                enqueue(new Node<>(e));
                c = count.getAndIncrement();
                if (c + 1 < capacity) {
                    notFull.signal();
                }
            }
        } finally {
            putLock.unlock();
        }
        if (c == 0) {
            signalNotEmpty();
        }
        return c >= 0;
    }


    @Override
    public E take() throws InterruptedException {
        E x;
        int c = -1;
        final ReentrantLock takeLock = this.takeLock;
        final AtomicInteger count = this.count;
        takeLock.lockInterruptibly();
        try {
            while (count.get() == 0) {
                notEmpty.await();
            }
            x = dequeue();
            c = count.getAndDecrement();
            if (c > 1) {
                notEmpty.signal();
            }
        } finally {
            takeLock.unlock();
        }
        if (c == capacity) {
            signalNotFull();
        }
        return x;
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        E x;
        int c = -1;
        long nanos = unit.toNanos(timeout);
        final ReentrantLock takeLock = this.takeLock;
        final AtomicInteger count = this.count;
        takeLock.lockInterruptibly();
        try {
            while (count.get() == 0) {
                if (nanos <= 0) {
                    return null;
                }
                nanos = notEmpty.awaitNanos(timeout);
            }
            x = dequeue();
            c = count.getAndDecrement();
            if (c > 1) {
                notEmpty.signal();
            }
        } finally {
            takeLock.unlock();
        }
        if (c == capacity) {
            signalNotFull();
        }
        return x;
    }

    @Override
    public E poll() {
        final AtomicInteger count = this.count;
        if (count.get() == 0) {
            return null;
        }
        E x = null;
        int c = -1;
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lock();
        try {
            if (count.get() > 0) {
                x = dequeue();
                c = count.getAndIncrement();
                if (c > 1) {
                    notEmpty.signal();
                }
            }
        } finally {
            takeLock.unlock();
        }
        if (c == capacity) {
            signalNotFull();
        }
        return x;
    }

    @Override
    public E peek() {
        if (count.get() == 0) {
            return null;
        }
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lock();
        try {
            Node<E> first = head.next;
            if (first == null) {
                return null;
            } else {
                return first.item;
            }
        } finally {
            takeLock.unlock();
        }
    }

    /**
     * 撤销内部节点p与前置节点trail的链接
     * @param p
     * @param trail
     */
    void unlink(Node<E> p, Node<E> trail) {
        p.item = null;
        trail.next = p.next;
        if (last == p) {
            last = trail;
        }
        if (count.getAndDecrement() == capacity) {
            notFull.signal();
        }
    }

    @Override
    public boolean remove(Object o) {
        if (o == null) {
            return false;
        }
        fullyLock();
        try {
            for(Node<E> trail = head, p = trail.next; p != null; trail = p.next, p = trail.next) {
                if (o.equals(p.item)) {
                    unlink(p, trail);
                    return true;
                }
            }
            return false;
        } finally {
            fullyUnlock();
        }
    }

    @Override
    public boolean contains(Object o) {
        if (o == null) {
            return false;
        }
        fullyLock();
        try {
            for (Node<E> p = head.next; p != null; p = p.next) {
                if (o.equals(p.item)) {
                    return true;
                }
            }
            return false;
        } finally {
            fullyUnlock();
        }
    }

    @Override
    public Object[] toArray() {
        fullyLock();
        try {
            int size = count.get();
            Object[] a = new Object[size];
            int k = 0;
            for (Node<E> p = head.next; p != null; p = p.next) {
                a[k++] = p.item;
            }
            return a;
        } finally {
            fullyUnlock();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T[] toArray(T[] a) {
        fullyLock();
        try {
            int size = count.get();
            if (a.length < size) {
                a = (T[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size);
            }
            int k = 0;
            for (Node<E> p = head.next; p != null; p = p.next) {
                a[k++] = (T) p.item;
            }
            if (a.length > k) {
                a[k] = null;
            }
            return a;
        } finally {
            fullyUnlock();
        }
    }

    @Override
    public void clear() {
        fullyLock();
        try {
            for (Node<E> p, h = head; (p = h.next) != null; h = p) {
                h.next = h;
                p.item = null;
            }
            head = last;
            if (count.getAndSet(0) == capacity) {
                notFull.signal();
            }
        } finally {
            fullyUnlock();
        }
    }

    @Override
    public String toString() {
        fullyLock();
        try {
            Node<E> p = head.next;
            if (p == null) {
                return "[]";
            }
            StringBuilder sb = new StringBuilder();
            sb.append('[');
            for (;;) {
                E e = p.item;
                sb.append(e == this ? "(this Collection)" : e);
                p = p.next;
                if (p == null) {
                    return sb.append(']').toString();
                }
                sb.append(',').append(' ');
            }
        } finally {
            fullyUnlock();
        }
    }

    @Override
    public int drainTo(Collection<? super E> c) {
        return drainTo(c, Integer.MAX_VALUE);
    }

    @Override
    public int drainTo(Collection<? super E> c, int maxElements) {
        if (c == null) {
            throw new NullPointerException();
        }
        if (c == this) {
            throw new IllegalArgumentException();
        }
        if (maxElements <= 0) {
            return 0;
        }
        boolean signalNotFull = false;
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lock();
        try {
            int n = Math.min(maxElements, count.get());
            Node<E> h = head;
            int i = 0;
            try {
                while (i < n) {
                    Node<E> p = h.next;
                    c.add(p.item);
                    p.item = null;
                    h.next = h;
                    h = p;
                    i++;
                }
                return n;
            } finally {
                if (i > 0) {
                    head = h;
                    signalNotFull = (count.getAndAdd(-i) == capacity);
                }
            }
        } finally {
            takeLock.unlock();
            if (signalNotFull) {
                signalNotFull();
            }
        }
    }

    @Override
    public Iterator<E> iterator() {
        return new Itr();
    }

    /**
     * 基本的弱一致的迭代器
     */
    private class Itr implements Iterator<E> {

        private Node<E> current;
        private Node<E> lastRet;
        private E currentElement;

        Itr() {
            fullyLock();
            try {
                current = head.next;
                if (current != null) {
                    currentElement = current.item;
                }
            } finally {
                fullyUnlock();
            }
        }

        @Override
        public boolean hasNext() {
            return current != null;
        }

        /**
         *
         * @param p
         * @return
         */
        private Node<E> nextNode(Node<E> p) {
            for(;;) {
                Node<E> s = p.next;
                if (s == p) {
                    return head.next;
                }
                if (s == null || s.item != null) {
                    return s;
                }
                p = s;
            }
        }

        @Override
        public E next() {
            fullyLock();
            try {
                if (current == null) {
                    throw new NullPointerException();
                }

                E x = currentElement;
                lastRet = current;
                current = nextNode(current);
                currentElement = current == null ? null : current.item;
            } finally {
                fullyUnlock();
            }
            return null;
        }

        @Override
        public void remove() {
            if (lastRet == null) {
                throw new IllegalStateException();
            }

            fullyLock();
            try {
                Node<E> node = lastRet;
                lastRet = null;
                for (Node<E> trail = head, p = trail.next; p != null; trail = p, p = trail.next) {
                    if (p == node) {
                        unlink(p, trail);
                        break;
                    }
                }
            } finally {
              fullyUnlock();
            }
        }
    }

    /**
     * 自定义Spliterators.IteratorSpliterator
     * @param <E>
     */
    static final class LBQSpliterator<E> implements Spliterator<E> {
        // 最大批次数组大小
        static final int MAX_BATCH = 1 << 25;

        final ResizableLinkedBlockingQueue<E> queue;

        // 当前节点
        Node<E> current;

        // 批处理大小
        int batch;

        boolean exhausted;

        // 队列大小的估值
        long est;

        LBQSpliterator(ResizableLinkedBlockingQueue<E> queue) {
            this.queue = queue;
            this.est = queue.size();
        }

        @Override
        public long estimateSize() {
            return est;
        }

        @Override
        public Spliterator<E> trySplit() {
            Node<E> h;
            final ResizableLinkedBlockingQueue<E> q = this.queue;
            int b = batch;
            int n = (b <= 0) ? 1 : (b >= MAX_BATCH) ? MAX_BATCH : b + 1;
            if (!exhausted && ((h = current) != null || (h = q.head.next) != null) && h.next != null) {
                Object[] a = new Object[n];
                int i = 0;
                Node<E> p = current;
                q.fullyLock();
                try {
                    if (p != null || (p = q.head.next) != null) {
                        do {
                            if ((a[i] = p.item) != null)
                                ++i;
                        } while ((p = p.next) != null && i < n);
                    }
                } finally {
                    q.fullyUnlock();
                }
                if ((current = p) == null) {
                    est = 0L;
                    exhausted = true;
                }
                else if ((est -= i) < 0L)
                    est = 0L;
                if (i > 0) {
                    batch = i;
                    return Spliterators.spliterator(a, 0, i, Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.CONCURRENT);
                }
            }
            return null;
        }

        @Override
        public void forEachRemaining(Consumer<? super E> action) {
            if (action == null) throw new NullPointerException();
            final ResizableLinkedBlockingQueue<E> q = this.queue;
            if (!exhausted) {
                exhausted = true;
                Node<E> p = current;
                do {
                    E e = null;
                    q.fullyLock();
                    try {
                        if (p == null)
                            p = q.head.next;
                        while (p != null) {
                            e = p.item;
                            p = p.next;
                            if (e != null)
                                break;
                        }
                    } finally {
                        q.fullyUnlock();
                    }
                    if (e != null)
                        action.accept(e);
                } while (p != null);
            }
        }

        @Override
        public boolean tryAdvance(Consumer<? super E> action) {
            if (action == null) throw new NullPointerException();
            final ResizableLinkedBlockingQueue<E> q = this.queue;
            if (!exhausted) {
                E e = null;
                q.fullyLock();
                try {
                    if (current == null)
                        current = q.head.next;
                    while (current != null) {
                        e = current.item;
                        current = current.next;
                        if (e != null)
                            break;
                    }
                } finally {
                    q.fullyUnlock();
                }
                if (current == null)
                    exhausted = true;
                if (e != null) {
                    action.accept(e);
                    return true;
                }
            }
            return false;
        }

        @Override
        public int characteristics() {
            return Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.CONCURRENT;
        }
    }

    @Override
    public Spliterator<E> spliterator() {
        return new LBQSpliterator<E>(this);
    }

    private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException{
        fullyLock();
        try {
            s.defaultWriteObject();
            for (Node<E> p = head.next; p != null; p = p.next) {
                s.writeObject(p.item);
            }
            // 队列尾部，可以看作一个哨兵
            s.writeObject(null);
        } finally {
            fullyUnlock();
        }
    }

    private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
        s.defaultReadObject();
        count.set(0);
        last = head = new Node<E>(null);
        for (;;) {
            E item = (E)s.readObject();
            if (item == null)
                break;
            add(item);
        }
    }
}
