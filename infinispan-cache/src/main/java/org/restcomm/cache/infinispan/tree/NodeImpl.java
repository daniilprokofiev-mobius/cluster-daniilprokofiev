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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.infinispan.AdvancedCache;
import org.infinispan.atomic.AtomicMap;
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
public class NodeImpl extends TreeStructureSupport implements Node {
	TreeSegment<?> fqn;
	TreeCache realCache;
	
	public NodeImpl(TreeCache realCache,TreeSegment<?> fqn, AdvancedCache<TreeSegment<?>, AtomicMap<Object, Object>> cache) {
		super(cache);
		this.realCache=realCache;		
		this.fqn = fqn;
	}

	@Override
	public Set<Entry<TreeSegment<?>,Object>> getChildren() {
		return getChildren(cache);
	}

	@Override
	public Set<Entry<TreeSegment<?>,Object>> getChildren(Flag... flags) {
		return getChildren(cache.withFlags(flags));
	}

	private Set<Entry<TreeSegment<?>,Object>> getChildren(AdvancedCache<TreeSegment<?>, AtomicMap<Object, Object>> cache) {
		Set<Entry<TreeSegment<?>,Object>> result = new HashSet<Entry<TreeSegment<?>,Object>>();
		if(fqn.isRoot()) {
			List<TreeSegment<?>> realSegments=realCache.getAllRootSegment();
			for(TreeSegment<?> currSegment:realSegments) {
				Iterator<Entry<Object,Object>> allChilds = getStructure(cache, currSegment).entrySet().iterator();
				while (allChilds.hasNext()) {
					Entry<Object,Object> currEntry = allChilds.next();
					if (!(currEntry.getKey() instanceof Boolean)) {
						result.add(new NodeEntry<TreeSegment<?>,Object>(fqn.getChild(currEntry.getKey()),currEntry.getValue()));						
					}
				}
			}
		}
		else {
			Iterator<Entry<Object,Object>> allChilds = getStructure(cache, fqn).entrySet().iterator();
			while (allChilds.hasNext()) {
				Entry<Object,Object> currEntry = allChilds.next();
				if (!(currEntry.getKey() instanceof Boolean)) {
					result.add(new NodeEntry<TreeSegment<?>,Object>(fqn.getChild(currEntry.getKey()),currEntry.getValue()));					
				}
			}
		}
		
		return result;
	}

	@Override
	public Set<TreeSegment<?>> getChildrenNames() {
		return getChildrenNames(cache);
	}

	@Override
	public Set<TreeSegment<?>> getChildrenNames(Flag... flags) {
		return getChildrenNames(cache.withFlags(flags));
	}

	private Set<TreeSegment<?>> getChildrenNames(AdvancedCache<TreeSegment<?>, AtomicMap<Object, Object>> cache) {
		Set<TreeSegment<?>> result=new HashSet<TreeSegment<?>>();
		if(fqn.isRoot()) {
			List<TreeSegment<?>> realSegments=realCache.getAllRootSegment();
			for(TreeSegment<?> currSegment:realSegments) {
				Set<Object> allChilds=getStructure(cache, currSegment).keySet();		
				for(Object curr:allChilds) {
					if(!(curr instanceof Boolean))
						result.add(fqn.getChild(curr));
				}
			}
		}
		else {
			Set<Object> allChilds=getStructure(cache, fqn).keySet();		
			for(Object curr:allChilds) {
				if(!(curr instanceof Boolean))
					result.add(fqn.getChild(curr));
			}
		}
		
		return result;
	}
	


	@Override
	public Boolean hasChildren() {
		return hasChildren(cache);
	}

	@Override
	public Boolean hasChildren(Flag... flags) {
		return hasChildren(cache.withFlags(flags));
	}

	private Boolean hasChildren(AdvancedCache<TreeSegment<?>, AtomicMap<Object, Object>> cache) {
		if(fqn.isRoot()) {
			List<TreeSegment<?>> realSegments=realCache.getAllRootSegment();
			for(TreeSegment<?> currSegment:realSegments) {
				if(!getStructure(cache, currSegment).isEmpty())
					return true;				
			}
			
			return false;
		}
		else
			return !getStructure(cache, fqn).isEmpty();		
	}

	private Object getData(AdvancedCache<TreeSegment<?>, AtomicMap<Object, Object>> cache, TreeSegment<?> f) {
		if(fqn.isRoot())
			return null;
		
		return getStructure(cache, fqn).get(f.getSegment());
	}

	@Override
	public TreeSegment<?> getFqn() {
		return fqn;
	}

	@Override
	public Node addChild(TreeSegment<?> f, Boolean createIfExists) {
		return addChild(cache, createIfExists, f);
	}

	@Override
	public Node addChild(TreeSegment<?> f, Boolean createIfExists, Flag... flags) {
		return addChild(cache.withFlags(flags), createIfExists, f);
	}

	private Node addChild(AdvancedCache<TreeSegment<?>, AtomicMap<Object, Object>> cache, Boolean createIfExists, TreeSegment<?> f) {
		// 1) first register it with the parent
		AtomicMap<Object, Object> structureMap;
		if(fqn.isRoot())
			structureMap = getStructure(cache, realCache.getRootSegment(f.hashCode()));
		else
			structureMap = getStructure(cache, fqn);
	
		if(!createIfExists) {
			if(structureMap.putIfAbsent(f.getSegment(), BOOLEAN_ELEMENT)!=null)
				return null;
		}
		else
			structureMap.put(f.getSegment(), BOOLEAN_ELEMENT);
				
		// 2) then create the structure and data maps
		createNodeInCache(cache, f);

		return new NodeImpl(realCache, f, cache);
	}

	@Override
	public boolean removeChild(TreeSegment<?> f) {
		return removeChild(cache, f);
	}

	@Override
	public boolean removeChild(TreeSegment<?> f, Flag... flags) {
		return removeChild(cache.withFlags(flags), f);
	}

	public boolean removeChild(AdvancedCache<TreeSegment<?>, AtomicMap<Object, Object>> cache, TreeSegment<?> f) {
		AtomicMap<Object, Object> s;
		if(fqn.isRoot())						
			s = getStructure(cache, realCache.getRootSegment(f.hashCode()));
		else
			s = getStructure(cache, fqn);
		
		Object childValue = s.remove(f.getSegment());
		if (childValue != null) {
			if(childValue.equals(BOOLEAN_ELEMENT)) {
				//only if its boolean element it may be non leaf
				Node child = new NodeImpl(realCache, f, cache);
				child.removeChildren();	
				
				cache.remove(f);				
			}

			return true;
		}

		return false;
	}

	@Override
	public Node getChild(TreeSegment<?> f) {
		return getChild(cache, f);
	}

	@Override
	public Node getChild(TreeSegment<?> f, Flag... flags) {
		return getChild(cache.withFlags(flags), f);
	}

	private Node getChild(AdvancedCache<TreeSegment<?>, AtomicMap<Object, Object>> cache, TreeSegment<?> f) {
		if (hasChild(f))
			return new NodeImpl(realCache, f, cache);
		else
			return null;
	}

	@Override
	public Object put(TreeSegment<?> f, Object value) {
		return put(cache, f, value);
	}

	@Override
	public Object put(TreeSegment<?> f, Object value, Flag... flags) {
		return put(cache.withFlags(flags), f, value);
	}

	private Object put(AdvancedCache<TreeSegment<?>, AtomicMap<Object, Object>> cache, TreeSegment<?> f, Object value) {
		if(fqn.isRoot())
			return null;
		
		AtomicMap<Object, Object> map = getStructure(cache, fqn);
		return map.put(f.getSegment(), value);
	}

	@Override
	public Object putIfAbsent(TreeSegment<?> f, Object value) {
		return putIfAbsent(cache, f, value);
	}

	@Override
	public Object putIfAbsent(TreeSegment<?> f, Object value, Flag... flags) {
		return putIfAbsent(cache.withFlags(flags), f, value);
	}

	private Object putIfAbsent(AdvancedCache<TreeSegment<?>, AtomicMap<Object, Object>> cache, TreeSegment<?> f, Object value) {
		if(fqn.isRoot())
			return null;
		
		AtomicMap<Object, Object> map = getStructure(cache, fqn);
		return map.putIfAbsent(f.getSegment(), value);
	}

	@Override
	public Object remove(TreeSegment<?> f) {
		return remove(cache, f);
	}

	@Override
	public Object remove(TreeSegment<?> f, Flag... flags) {
		return remove(cache.withFlags(flags), f);
	}

	private Object remove(AdvancedCache<TreeSegment<?>, AtomicMap<Object, Object>> cache, TreeSegment<?> f) {
		if(fqn.isRoot())
			return null;
		
		AtomicMap<Object, Object> map = getStructure(cache, fqn);
		return map.remove(f.getSegment());
	}

	@Override
	public Object get(TreeSegment<?> f) {
		return get(cache, f);
	}

	@Override
	public Object get(TreeSegment<?> f, Flag... flags) {
		return get(cache.withFlags(flags), f);
	}

	private Object get(AdvancedCache<TreeSegment<?>, AtomicMap<Object, Object>> cache, TreeSegment<?> f) {
		return getData(cache, f);
	}

	@Override
	public boolean hasChild(TreeSegment<?> f) {
		return hasChild(cache, f);
	}

	@Override
	public boolean hasChild(TreeSegment<?> f, Flag... flags) {
		return hasChild(cache.withFlags(flags), f);
	}

	private boolean hasChild(AdvancedCache<TreeSegment<?>, AtomicMap<Object, Object>> cache, TreeSegment<?> f) {
		AtomicMap<Object, Object> map;
		if(fqn.isRoot()) {
			map = getStructure(cache, realCache.getRootSegment(f.hashCode()));
		}
		else
			map = getStructure(cache, fqn);
		
		return map.containsKey(f.getSegment());
	}

	@Override
	public boolean isValid() {
		return cache.containsKey(fqn);
	}

	@Override
	public void removeChildren() {
		removeChildren(cache);
	}

	@Override
	public void removeChildren(Flag... flags) {
		removeChildren(cache.withFlags(flags));
	}

	private void removeChildren(AdvancedCache<TreeSegment<?>, AtomicMap<Object, Object>> cache) {
		if(fqn.isRoot()) {
			List<TreeSegment<?>> realSegments=realCache.getAllRootSegment();
			for(TreeSegment<?> curr:realSegments) {
				Map<Object, Object> s = getStructure(cache, curr);
				Map.Entry<?,?>[] realData=new Entry<?,?>[s.size()];
				realData=s.entrySet().toArray(realData);
				for(Entry<?,?> currEntry:realData) {
					Object childValue=currEntry.getValue();
					if (childValue != null) {
						if(childValue.equals(BOOLEAN_ELEMENT)) {
							TreeSegment<?> f=fqn.getChild(currEntry.getKey());
							//only if its boolean element it may be non leaf
							Node child = new NodeImpl(realCache, f, cache);
							child.removeChildren();	
							
							cache.remove(f);				
						}
					}
				}
			}
		}
		else {
			Map<Object, Object> s = getStructure(cache, fqn);
			Map.Entry<?,?>[] realData=new Entry<?,?>[s.size()];
			realData=s.entrySet().toArray(realData);
			for(Entry<?,?> currEntry:realData) {
				Object childValue=currEntry.getValue();
				if (childValue != null) {
					if(childValue.equals(BOOLEAN_ELEMENT)) {
						TreeSegment<?> f=fqn.getChild(currEntry.getKey());
						//only if its boolean element it may be non leaf
						Node child = new NodeImpl(realCache, f, cache);
						child.removeChildren();	
						
						cache.remove(f);				
					}
				}
			}
		}
	}

	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		NodeImpl node = (NodeImpl) o;

		if (fqn != null ? !fqn.equals(node.fqn) : node.fqn != null)
			return false;

		return true;
	}

	public int hashCode() {
		return (fqn != null ? fqn.hashCode() : 0);
	}

	@Override
	public String toString() {
		return "NodeImpl{" + "fqn=" + fqn + '}';
	}
}
