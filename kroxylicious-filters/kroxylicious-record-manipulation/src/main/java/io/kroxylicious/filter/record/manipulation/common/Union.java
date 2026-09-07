/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.filter.record.manipulation.common;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import io.leangen.geantyref.GenericTypeReflector;

public class Union implements Type {

    private final Set<Type> members;

    private Union(Set<Type> members) {
        this.members = members;
    }

    static Union of(Type type) {
        if (type instanceof Union union) {
            return union;
        }
        return new Union(Set.of(type));
    }

    static Union of(Type type, Type type2) {
        if (type instanceof Union || type2 instanceof Union) {
            return of(List.of(type, type2));
        }
        if (GenericTypeReflector.isSuperType(type, type2)) {
            return of(type);
        }
        else if (GenericTypeReflector.isSuperType(type2, type)) {
            return of(type2);
        }
        LinkedHashSet<Type> members1 = LinkedHashSet.newLinkedHashSet(2);
        members1.add(type);
        members1.add(type2);
        return new Union(members1);
    }

    static Union of(List<Type> types) {
        LinkedHashSet<Type> members = new LinkedHashSet<>();
        eliminateUnions(types, members);
        List<Type> remove = new ArrayList<>();
        for (var t1 : members) {
            for (var t2 : members) {
                if (!t1.equals(t2)) {
                    if (GenericTypeReflector.isSuperType(t1, t2)) {
                        remove.add(t2);
                    }
                    else if (GenericTypeReflector.isSuperType(t2, t1)) {
                        remove.add(t1);
                    }
                }
            }
        }
        members.removeAll(remove);
        return new Union(new LinkedHashSet<>(members));
    }

    Union add(Type type) {
        ArrayList<Type> members1;
        if (type instanceof Union union) {
            members1 = new ArrayList(this.members.size() + union.members.size());
            members1.addAll(this.members);
            members1.addAll(union.members);
        }
        else {
            members1 = new ArrayList(this.members.size() + 1);
            members1.addAll(this.members);
            members1.add(type);
        }
        return of(members1);
    }

    private static void eliminateUnions(Collection<Type> types, Collection<Type> members) {
        for (Type type : types) {
            if (type instanceof Union union) {
                eliminateUnions(union.members, members);
            }
            else {
                members.add(type);
            }
        }
    }

    public Set<Type> members() {
        return members;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Union union)) {
            return false;
        }
        return Objects.equals(members, union.members);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(members);
    }

    @Override
    public String toString() {
        return members.stream().map(Object::toString).collect(Collectors.joining("|"));
    }

    public boolean isSupertypeOf(Type type) {
        if (type instanceof Union otherUnion) {
            OUTER: for (Type otherType : otherUnion.members) {
                for (Type myType : members) {
                    if (GenericTypeReflector.isSuperType(myType, otherType)) {
                        continue OUTER;
                    }
                }
                return false;
            }
            return true;
        }
        for (var member : members) {
            if (GenericTypeReflector.isSuperType(member, type)) {
                return true;
            }
        }
        return false;
    }
}
