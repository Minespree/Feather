package net.minespree.feather.util.collections;

import com.google.common.base.Preconditions;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

public class WeightedTable<V> {
    private RangeMap<Double, V> range = TreeRangeMap.create();

    private double max = 0D;

    private double nullChance = -1;

    private Range<Double> last;

    @SafeVarargs
    public final WeightedTable<V> filter(boolean retainChances, Predicate<V>... filters) {
        return filter(retainChances, Arrays.asList(filters));
    }

    /**
     * Returns a WeightedRandomTable&lt;V&gt; which only contains elements <i>which return false on every filter</i>.
     *
     * @param retainChances Indicates whether to add a null entry to ensure the same chances on the retained elements.
     * @param filters       A collection of {@code Predicates} which indicate that an entry should be removed if they return {@code true}
     * @return WeightedRandomTable&lt;V&gt; with only elements which return false on all filters.
     */
    public WeightedTable<V> filter(boolean retainChances, Collection<Predicate<V>> filters) {
        WeightedTable<V> newTable = new WeightedTable<>();
        double sum = 0;
        for (Map.Entry<Range<Double>, V> entry : this.range.asMapOfRanges().entrySet()) {
            V value = entry.getValue();
            if (filters.stream().noneMatch(filter -> filter.test(value))) {
                continue;
            }
            Range<Double> range = entry.getKey();
            double chance = range.upperEndpoint() - range.lowerEndpoint();
            sum += chance;
            newTable.add(chance, value);
        }
        if (sum < this.max) {
            newTable.none(this.max - sum);
        }
        return newTable;
    }

    public void none(double chance) {
        Preconditions.checkArgument(chance > 0, "Chance has to be positive, was " + chance);
        this.nullChance += chance;
    }

    public void add(double chance, V element) {
        Preconditions.checkArgument(chance > 0, "Chance has to be positive, was " + chance);
        Preconditions.checkNotNull(element, "Element has to be nonnull - use WeightedTable#none(double) to add nulls");
        Range<Double> range = Range.closedOpen(max, max + chance);
        this.range.put(range, element);
        this.max += chance;
    }

    /**
     * Fetches a random element from the WeightedRandomTable
     *
     * @return An {@code Optional} containing a random entry if found, empty {@code Optional} otherwise.
     */
    public Optional<V> getRandom() {
        return getRandom(true);
    }

    public Optional<V> getRandom(boolean includeNull) {
        return getRandom(includeNull, false);
    }

    /**
     * Fetches a random element from the WeightedRandomTable
     *
     * @param excludeLast Exclude the last entry returned by this WeightedRandomTable
     * @return
     */
    public Optional<V> getRandom(boolean includeNull, boolean excludeLast) {
        if (this.nullChance < 0) {
            this.nullChance = 100 - Math.min(this.max, 100);
        }
        double queryMax = this.max, add = 0;
        if (includeNull) {
            queryMax += this.nullChance;
        }
        excludeLast &= this.last != null;
        if (excludeLast) {
            add = this.last.upperEndpoint() - this.last.lowerEndpoint();
            queryMax -= add;
        }
        double d = ThreadLocalRandom.current().nextDouble() * queryMax;
        if (excludeLast && this.last.contains(d)) {
            d += add;
        }
        Map.Entry<Range<Double>, V> entry = null;
        // Else it falls into the null case
        if (d < this.max) {
            entry = this.range.getEntry(d);
        }
        this.last = entry != null ? entry.getKey() : null;
        return Optional.ofNullable(entry != null ? entry.getValue() : null);
    }
}
