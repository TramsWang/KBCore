package sinc.common;

import sinc.util.MultiSet;

import java.util.*;

public class RuleFingerPrint {
    private final String headFunctor;
    private final MultiSet<ArgIndicator>[] headEquivClasses;
    private final int[] headEquivClassIds;
    private final Map<Integer, MultiSet<ArgIndicator>> allEquivClasses = new HashMap<>();

    public RuleFingerPrint(String headFunctor, int[] headIds) {
        this.headFunctor = headFunctor;
        headEquivClasses = new MultiSet[headIds.length];
        for (int i = 0; i < headEquivClasses.length; i++) {
            headEquivClasses[i] = new MultiSet<>();
            allEquivClasses.put(headIds[i], headEquivClasses[i]);
        }
        headEquivClassIds = headIds.clone();
    }

    public RuleFingerPrint(RuleFingerPrint another) {
        this.headFunctor = another.headFunctor;
        this.headEquivClasses = new MultiSet[another.headEquivClasses.length];
        this.headEquivClassIds = another.headEquivClassIds.clone();
        for (int i = 0; i < another.headEquivClasses.length; i++) {
            this.headEquivClasses[i] = new MultiSet<>(another.headEquivClasses[i]);
        }
        for (Map.Entry<Integer, MultiSet<ArgIndicator>> entry: another.allEquivClasses.entrySet()) {
            this.allEquivClasses.put(entry.getKey(), new MultiSet<>(entry.getValue()));
        }
    }

    public void createPredicate(String functor, int[] argIds) {
        for (int i = 0; i < argIds.length; i++) {
            MultiSet<ArgIndicator> equiv_class = new MultiSet<>();
            equiv_class.add(new VarIndicator(functor, i));
            allEquivClasses.put(argIds[i], equiv_class);
        }
    }

    public void assignConstant(int varId, String constant) {
        MultiSet<ArgIndicator> equiv_class = allEquivClasses.get(varId);
        if (null != equiv_class) {
            equiv_class.add(new ConstIndicator(constant));
        }
    }

    public void assignBoundedVar(int boundedVarId, int freeVarId) {
        MultiSet<ArgIndicator> bounded_equiv_class = allEquivClasses.get(boundedVarId);
        MultiSet<ArgIndicator> free_equiv_class = allEquivClasses.get(boundedVarId);
        if (null != bounded_equiv_class && null != free_equiv_class) {
            bounded_equiv_class.addAll(free_equiv_class);
            replaceEquivClass(freeVarId, boundedVarId);
        }
    }

    public void assignNewBoundedVar(int boundedVarId, int freeVarId1, int freeVarId2) {
        MultiSet<ArgIndicator> free_equiv_class1 = allEquivClasses.get(freeVarId1);
        MultiSet<ArgIndicator> free_equiv_class2 = allEquivClasses.get(freeVarId2);
        if (null != free_equiv_class1 && null != free_equiv_class2) {
            MultiSet<ArgIndicator> new_equiv_class = new MultiSet<>();
            new_equiv_class.addAll(free_equiv_class1);
            new_equiv_class.addAll(free_equiv_class2);
            allEquivClasses.put(boundedVarId, new_equiv_class);
            replaceEquivClass(freeVarId1, boundedVarId);
            replaceEquivClass(freeVarId2, boundedVarId);
        }
    }

    public void changeVar(int originalVarid, int newVarId) {
        MultiSet<ArgIndicator> original_equiv_class = allEquivClasses.remove(originalVarid);
        allEquivClasses.put(newVarId, original_equiv_class);
        for (int i = 0; i < headEquivClassIds.length; i++) {
            if (originalVarid == headEquivClassIds[i]) {
                headEquivClassIds[i] = newVarId;
            }
        }
    }

    private void replaceEquivClass(int originalEquivClassId, int newEquivClassId) {
        /* 检查Head EC 列表中可能被替换的项 */
        MultiSet<ArgIndicator> original_equiv_class = allEquivClasses.remove(originalEquivClassId);
        for (int i = 0; i < headEquivClassIds.length; i++) {
            if (originalEquivClassId == headEquivClassIds[i]) {
                headEquivClassIds[i] = newEquivClassId;
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RuleFingerPrint that = (RuleFingerPrint) o;
        if (!this.headFunctor.equals(that.headFunctor) ||
                this.headEquivClasses.length != that.headEquivClasses.length ||
                this.allEquivClasses.size() != that.allEquivClasses.size()) {
            return false;
        }
        for (int i = 0; i < headEquivClasses.length; i++) {
            if (!this.headEquivClasses[i].equals(that.headEquivClasses[i])) {
                return false;
            }
        }
        Set<MultiSet<ArgIndicator>> this_ec_set = new HashSet<>(this.allEquivClasses.values());
        for (MultiSet<ArgIndicator> that_ec: that.allEquivClasses.values()) {
            if (!this_ec_set.contains(that_ec)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(headFunctor, allEquivClasses);
        result = 31 * result + Arrays.hashCode(headEquivClasses);
        return result;
    }
}
