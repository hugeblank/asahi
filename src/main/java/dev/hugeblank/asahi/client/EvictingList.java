package dev.hugeblank.asahi.client;

import java.util.AbstractList;
import java.util.function.Consumer;

public class EvictingList<T> extends AbstractList<T> {

    private final int arrSize;
    private int size = 0;
    private final T[] array;

    public EvictingList(int size) {
        this.arrSize = size;
        //noinspection unchecked
        this.array = (T[]) new Object[arrSize];
    }

    @Override
    public boolean add(T element) {
        for (int i = size; i > 0; --i) {
            if (i < arrSize) {
                array[i] = array[i-1];
            }
        }
        array[0] = element;
        size = Math.min(size+1, arrSize);
        return true;
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        for (int i = 0; i < size; i++) {
            action.accept(array[i]);
        }
    }

    @Override
    public T get(int index) {
        if (index < 0 || index >= size) throw new ArrayIndexOutOfBoundsException();
        return array[index];
    }

    @Override
    public int size() {
        return size;
    }
}
