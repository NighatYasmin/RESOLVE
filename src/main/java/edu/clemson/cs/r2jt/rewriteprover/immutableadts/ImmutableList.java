/*
 * ImmutableList.java
 * ---------------------------------
 * Copyright (c) 2020
 * RESOLVE Software Research Group
 * School of Computing
 * Clemson University
 * All rights reserved.
 * ---------------------------------
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.txt', which is part of this source code package.
 */
package edu.clemson.cs.r2jt.rewriteprover.immutableadts;

import java.util.Iterator;

public interface ImmutableList<E> extends Iterable<E> {

    /*
     * "Mutator" methods that return a new, changed version of this list
     */

    public ImmutableList<E> appended(E e);

    public ImmutableList<E> appended(ImmutableList<E> l);

    public ImmutableList<E> appended(Iterable<E> i);

    public ImmutableList<E> removed(int index);

    public ImmutableList<E> set(int index, E e);

    public ImmutableList<E> insert(int index, E e);

    public ImmutableList<E> insert(int index, ImmutableList<E> e);

    /*
     * Methods that return a view of this list
     */

    public ImmutableList<E> subList(int startIndex, int length);

    public ImmutableList<E> tail(int startIndex);

    public ImmutableList<E> head(int length);

    /*
     * Methods for getting out elements
     */

    public E first();

    public E get(int index);

    public Iterator<E> iterator();

    /*
     * Utility methods.
     */

    public int size();
}
