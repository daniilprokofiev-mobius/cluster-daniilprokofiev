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
import java.util.Set;

import org.infinispan.context.Flag;
import org.restcomm.cluster.data.TreeSegment;

/*
 * This is modified copy of infinispan implementation for tree
 * following changes are done
 * minimzed support for operations -> some are not required for the project
 * number of root nodes is configurable. This is because the root node becomes
 * main botleneck for the perfomance
 * The number of nodes per tree has been decreased from 2 to 1 
 * This implementation supports either data nodes or tree nodes while infinispan implementation
 * supported mixed mode
 */
/**
 * @author yulian.oifa
 */
public interface Node {
	/**
	 * Returns an immutable set of data items.
	 *
	 * @return an immutable {@link Set} of data items. Empty {@link Set} if there
	 *         aren't any data items.
	 */
	Set<Entry<TreeSegment<?>,Object>> getChildren();

	Set<Entry<TreeSegment<?>,Object>> getChildren(Flag... flags);

	/**
	 * Returns an immutable set of children node names.
	 *
	 */
	Set<TreeSegment<?>> getChildrenNames();

	Set<TreeSegment<?>> getChildrenNames(Flag... flags);

	/**
	 * Checks if any children exists.
	 *
	 * @return an whether of child node names.
	 */
	Boolean hasChildren();

	Boolean hasChildren(Flag... flags);
	
	/**
	 * Returns the {@link Fqn} which represents the location of this {@link Node} in
	 * the cache structure. The {@link Fqn} returned is absolute.
	 *
	 * @return The {@link Fqn} which represents the location of this {@link Node} in
	 *         the cache structure. The {@link Fqn} returned is absolute.
	 */
	TreeSegment<?> getFqn();

	/**
	 * Adds a child node with the given {@link Fqn} under the current node. Returns
	 * the newly created node.
	 * <p/>
	 * If the child exists returns the child node anyway. Guaranteed to return a
	 * non-null node.
	 * <p/>
	 * The {@link Fqn} passed in is relative to the current node. The new child node
	 * will have an absolute fqn calculated as follows:
	 * 
	 * <pre>
	 * new Fqn(getFqn(), f)
	 * </pre>
	 * 
	 * . See {@link Fqn} for the operation of this constructor.
	 *
	 * @param f {@link Fqn} of the child node, relative to the current node.
	 * @return the newly created node, or the existing node if one already exists.
	 */
	Node addChild(TreeSegment<?> f, Boolean createIfExists);

	Node addChild(TreeSegment<?> f, Boolean createIfExists, Flag... flags);

	/**
	 * Removes a child node specified by the given relative {@link Fqn}.
	 * <p/>
	 * If you wish to remove children based on absolute {@link Fqn}s, use the
	 * {@link TreeCache} interface instead.
	 *
	 * @param f {@link Fqn} of the child node, relative to the current node.
	 * @return true if the node was found and removed, false otherwise
	 */
	boolean removeChild(TreeSegment<?> f);

	boolean removeChild(TreeSegment<?> f, Flag... flags);
	
	/**
	 * Returns the child node
	 *
	 * @param f {@link Fqn} of the child node
	 * @return null if the child does not exist.
	 */
	Node getChild(TreeSegment<?> f);

	Node getChild(TreeSegment<?> f, Flag... flags);
	
	/**
	 * Associates the specified value with the specified key for this node. If this
	 * node previously contained a mapping for this key, the old value is replaced
	 * by the specified value.
	 *
	 * @param f key to associate value with
	 * @param value value to be associated with the specified key.
	 * @return Returns the old value contained under this key. Null if key doesn't
	 *         exist.
	 */
	Object put(TreeSegment<?> f, Object value);

	Object put(TreeSegment<?> f, Object value, Flag... flags);

	/**
	 * Associates the specified value with the specified key for this node. only if this
	 * node previously did not contained a mapping for this key.
	 *
	 * @param f key to associate value with
	 * @param value value to be associated with the specified key.
	 * @return Returns the old value contained under this key. Null if key doesn't
	 *         exist.
	 */
	Object putIfAbsent(TreeSegment<?> f, Object value);

	Object putIfAbsent(TreeSegment<?> f, Object value, Flag... flags);

	/**
	 * Removed the value associated with the specified key for this node.
	 *
	 * @param f key to associate value with
	 * @return Returns the old value contained under this key. Null if key doesn't
	 *         exist.
	 */
	Object remove(TreeSegment<?> f);

	Object remove(TreeSegment<?> f, Flag... flags);

	/**
	 * Returns the value to which this node maps the specified key. Returns
	 * <code>null</code> if the node contains no mapping for this key.
	 *
	 * @param f key to get value for
	 * @return the value to which this node maps the specified key, or
	 *         <code>null</code> if the map contains no mapping for this key
	 */
	Object get(TreeSegment<?> f);

	Object get(TreeSegment<?> f, Flag... flags);

	/**
	 * Returns true if the child node denoted by the relative {@link Fqn} passed in
	 * exists.
	 *
	 * @param f {@link Fqn} relative to the current node of the child you are
	 *          testing the existence of.
	 * @return true if the child node denoted by the relative {@link Fqn} passed in
	 *         exists.
	 */
	boolean hasChild(TreeSegment<?> f);

	boolean hasChild(TreeSegment<?> f, Flag... flags);

	/**
	 * Tests if a node reference is still valid. A node reference may become invalid
	 * if it has been removed, invalidated or moved, either locally or remotely. If
	 * a node is invalid, it should be fetched again from the cache or a valid
	 * parent node. Operations on invalid nodes will throw a
	 * {@link org.infinispan.tree.NodeNotValidException}.
	 *
	 * @return true if the node is valid.
	 */
	boolean isValid();

	void removeChildren();

	void removeChildren(Flag... flags);	
}