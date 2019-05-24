package com.botsula.labs.arrayset;

import java.util.*;

public class NavigableArraySet<E extends Comparable<? super E>> extends ArraySet<E> implements NavigableSet<E> {

    public class DescendingIterator implements Iterator<E> {
        private List<E> elementsList;
        private int from, to;

        public DescendingIterator(List<E> elementsList) {
            this.elementsList = elementsList;
            from = 0;
            to = elementsList.size() - 1;
        }

        @Override
        public boolean hasNext() {
            return to > from;
        }

        @Override
        public E next() {
            return elementsList.get(to--);
        }

    }

    public NavigableArraySet() {
        super();
    }

    public NavigableArraySet(Collection<E> collection) {
        super(collection);
    }

    public NavigableArraySet(Collection<E> collection, Comparator<E> comparator) {
        super(collection, comparator);
    }

    private E getElement(int index) {
        if (index >= 0 && index < this.elementsList.size()) {
            return elementsList.get(index);
        } else {
            return null;
        }
    }

    @Override
    public Iterator<E> descendingIterator() {
        return new DescendingIterator(elementsList);
    }

    @Override
    public NavigableSet<E> descendingSet() {
        return new NavigableArraySet<>(elementsList, Collections.reverseOrder(comparator));
    }

    @Override
    public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        int fromSearched = Collections.binarySearch(elementsList, fromElement, comparator);
        int toSearched = Collections.binarySearch(elementsList, toElement, comparator);
        int fromIndex = fromSearched < 0 ? (-fromSearched - 1) : fromSearched;
        int toIndex = toSearched < 0 ? (-toSearched - 1) : toSearched;

        if (fromIndex > toIndex) {
            throw new IllegalArgumentException();
        }

        if (fromIndex == toIndex && !(fromInclusive && toInclusive) && (fromSearched >= 0 && toSearched >= 0)) {
            return new NavigableArraySet<>(Collections.emptyList(), comparator);
        }

        if (!fromInclusive && fromSearched >= 0) {
            fromIndex++;
        }

        if (toInclusive && toSearched >= 0) {
            toIndex++;
        }

        return new NavigableArraySet<>(elementsList.subList(fromIndex, toIndex), comparator);

    }

    @Override
    public NavigableSet<E> tailSet(E fromElement, boolean fromInclusive) {
        int fromIndex = Collections.binarySearch(elementsList, fromElement, comparator);

        if (fromIndex < 0) {
            fromIndex = (-fromIndex - 1);
        } else if (!fromInclusive) {
            fromIndex++;
        }

        return new NavigableArraySet<>(elementsList.subList(fromIndex, size()), comparator);
    }

    @Override
    public NavigableSet<E> headSet(E toElement, boolean toInclusive) {
        int toIndex = Collections.binarySearch(elementsList, toElement, comparator);

        if (toIndex < 0) {
            toIndex = (-toIndex - 1);
        } else if (toInclusive) {
            toIndex++;
        }

        return new NavigableArraySet<>(this.elementsList.subList(0, toIndex), comparator);
    }

    @Override
    public E pollFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public E pollLast() {
        throw new UnsupportedOperationException();
    }

    @Override
    public E ceiling(E e) {
        int index = Collections.binarySearch(elementsList, e, comparator);
        index = index < 0 ? (-index - 1) : index;
        return getElement(index);
    }

    @Override
    public E higher(E e) {
        int index = Collections.binarySearch(elementsList, e, comparator);
        index = index < 0 ? (-index - 1) : index + 1;
        return getElement(index);
    }

    @Override
    public E floor(E e) {
        int index = Collections.binarySearch(elementsList, e, comparator);
        index = index < 0 ? (-index - 2) : index;
        return getElement(index);
    }

    @Override
    public E lower(E e) {
        int index = getElementIndex(e);
        index--;
        return getElement(index);
    }
}
