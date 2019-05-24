package com.botsula.labs.arrayset;

import java.util.*;

public class ArraySet<E extends Comparable<? super E>> extends AbstractSet<E> implements SortedSet<E> {
    protected List<E> elementsList;
    protected Comparator<E> comparator;

    public ArraySet() {
        this(new ArrayList<>(), null);
    }

    public ArraySet(Collection<E> collection) {
        this(new ArrayList<>(collection), null);
    }

    public ArraySet(Collection<E> collection, Comparator<E> comparator) {
        this.comparator = comparator;
        SortedSet<E> sortedSet = new TreeSet<>(comparator);
        sortedSet.addAll(collection);
        elementsList = new ArrayList<>(sortedSet);
    }

    public ArraySet(ArraySet<E> initialSet, int fromIndex, int toIndex) throws IllegalArgumentException {
        this.comparator = initialSet.comparator;
        this.elementsList = initialSet.elementsList.subList(fromIndex, toIndex);
    }

    protected int getElementIndex(E element) {
        int index = Collections.binarySearch(elementsList, element, comparator);
        return index < 0 ? -index - 1 : index;
    }

    @Override
    public Comparator<? super E> comparator() {
        return comparator;
    }

    @Override
    public SortedSet<E> headSet(E toElement) throws IndexOutOfBoundsException {
        int toIndex = getElementIndex(toElement);
        return new ArraySet<>(this, 0, toIndex);
    }

    @Override
    public SortedSet<E> subSet(E fromElement, E toElement) throws IndexOutOfBoundsException, IllegalArgumentException {
        int toIndex = getElementIndex(toElement);
        int fromIndex = getElementIndex(fromElement);
        Comparator<? super E> comparatorTemp = comparator != null ? comparator : Comparator.naturalOrder();
        if (comparatorTemp.compare(fromElement, toElement) > 0) {
            throw new IllegalArgumentException();
        }
        return new ArraySet<>(this, fromIndex, toIndex);
    }

    @Override
    public SortedSet<E> tailSet(E fromElement) throws IndexOutOfBoundsException {
        int fromIndex = getElementIndex(fromElement);
        return new ArraySet<>(this, fromIndex, elementsList.size());
    }

    @Override
    public Iterator<E> iterator() {
        return Collections.unmodifiableList(elementsList).iterator();
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(Object object) {
        return Collections.binarySearch(elementsList, (E) object, comparator) >= 0;
    }

    @Override
    public int size() throws NoSuchElementException {
        return elementsList.size();
    }

    @Override
    public E first() throws NoSuchElementException  {
        if(isEmpty())
            throw new NoSuchElementException();
        return elementsList.get(0);
    }

    @Override
    public E last() throws NoSuchElementException {
        if(isEmpty()) {
            throw new NoSuchElementException();
        }
        return elementsList.get(elementsList.size() - 1);
    }
}
