/*
 * Copyright 2022-2023, Mobius Software LTD. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package org.restcomm.cache.infinispan.tree;

import java.util.Map.Entry;

/**
 * @author yulian.oifa
 */
public class NodeEntry<K, V> implements Entry<K, V> 
{
    private final K key;
    private V value;

    public NodeEntry(K key, V value) 
    {
        this.key = key;
        this.value = value;
    }

    @Override
    public K getKey() 
    {
        return key;
    }

    @Override
    public V getValue() 
    {
        return value;
    }

    @Override
    public V setValue(V value) 
    {
        V old = this.value;
        this.value = value;
        return old;
    }
}