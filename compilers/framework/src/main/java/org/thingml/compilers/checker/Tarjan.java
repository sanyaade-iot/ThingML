/**
 * Copyright (C) 2014 SINTEF <franck.fleurey@sintef.no>
 *
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE, Version 3, 29 June 2007;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.gnu.org/licenses/lgpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thingml.compilers.checker;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.sintef.thingml.Configuration;
import org.sintef.thingml.Connector;
import org.sintef.thingml.Instance;
import org.sintef.thingml.Region;
import org.sintef.thingml.State;
import org.sintef.thingml.Thing;
import org.sintef.thingml.ThingMLElement;
import org.sintef.thingml.Transition;

/**
 *
 * @author sintef
 */
public class Tarjan<T extends ThingMLElement> {
        private class Annotated<T> {
            public T el;
            public int id, lowlink;
            public boolean isVisited;
            
            public Annotated(T el) {
                this.el = el;
                this.isVisited = false;
            }
            
            boolean equals(Annotated<T> other) {
                return EcoreUtil.equals((ThingMLElement) this.el, (ThingMLElement) other.el);
            }
            
        }
        
        int index;
        List<Annotated<T>> Stack;
        Set<Annotated<T>> vertices;
        Configuration cfg;
        List<List<T>> SCComponents;
        
        public Tarjan(Configuration cfg, Set<T> vertices) {
            this.cfg = cfg;
            this.vertices = new HashSet<Annotated<T>>();
            for(T el : vertices) {
                Annotated<T> Ael = new Annotated<T>(el);
                this.vertices.add(Ael);
            }
            index = 0;
            Stack = new LinkedList<Annotated<T>>();
            SCComponents = new LinkedList<List<T>>();
        }
        
        public Annotated<T> findElement(T el) {
            for(Annotated<T> Ael : vertices) {
                if(EcoreUtil.equals(Ael.el, el)) {
                    return Ael;
                }
            }
            return null;
        }
        
        public List<Annotated<T>> findChildren(T el) {
            List<Annotated<T>> res = new LinkedList<Annotated<T>>();
            if(el instanceof Instance) {
                for(Connector co : cfg.allConnectors()) {
                    if(EcoreUtil.equals(co.getCli().getInstance(), el)) {
                        res.add(findElement((T) co.getSrv().getInstance()));
                    }
                }
            } else {
                if(el instanceof State) {
                    State s = (State) el;
                    for (Transition tr : s.getOutgoing()) {
                        if(tr.getEvent().isEmpty()) {
                            if(tr.getGuard() == null) {
                                res.add(findElement((T) tr.getTarget()));
                            }
                        }
                    }
                }
            }
            return res;
        }
        
        public void StrongConnect(Annotated<T> v) {
            v.id = index;
            v.lowlink = index;
            v.isVisited = true;
            index++;
            Stack.add(0, v);
            
            for(Annotated<T> w : findChildren(v.el)) {
                    
                if(!w.isVisited) {
                    StrongConnect(w);
                    v.lowlink = Math.min(v.lowlink, w.lowlink);
                } else {
                    if(Stack.contains(w)) {
                        v.lowlink = Math.min(v.lowlink, w.id);
                    }
                }
            }
            
            if(v.id == v.lowlink) {
                List<T> res = new LinkedList<T>();
                Annotated<T> w;
                
                do {
                    w = Stack.get(0);
                    res.add(w.el);
                    Stack.remove(0);
                } while(!w.equals(v));
                SCComponents.add(res);
            }
        }
        
        public List<List<T>> findStronglyConnectedComponents() {
            for(Annotated<T> v : vertices) {
                if(!v.isVisited) {
                    StrongConnect(v);
                }
            }
            
            return SCComponents;
        }
    }