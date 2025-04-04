/*
 * Copyright 2015, 2024 StreamEx contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package one.util.streamex;

import org.junit.Test;

import java.util.*;
import java.util.function.Supplier;

import static one.util.streamex.TestHelpers.checkSpliterator;
import static one.util.streamex.TestHelpers.consumeElement;
import static org.junit.Assert.*;

public class IfEmptySpliteratorTest {
    @Test
    public void testSpliterator() {
        List<Integer> data = IntStreamEx.range(1000).boxed().toList();
        checkSpliterator("++", data, () -> new IfEmptySpliterator<>(data.spliterator(), data.spliterator()));
        checkSpliterator("-+", data, () -> new IfEmptySpliterator<>(Spliterators.emptySpliterator(), data.spliterator()));
        checkSpliterator("+-", data, () -> new IfEmptySpliterator<>(data.spliterator(), Spliterators.emptySpliterator()));
        checkSpliterator("--", Collections.emptyList(), () -> new IfEmptySpliterator<>(Spliterators.emptySpliterator(), Spliterators.emptySpliterator()));
    }

    @Test
    public void testCharacteristics() {
        Spliterator<String> sortedSpltr = StreamEx.of("foo", "bar", "baz").sorted().ifEmpty(
            StreamEx.of("foo", "bar", "baz").sorted()).spliterator();
        assertFalse(sortedSpltr.hasCharacteristics(Spliterator.SORTED));
        consumeElement(sortedSpltr, "bar");
        assertFalse(sortedSpltr.hasCharacteristics(Spliterator.SORTED));
        assertTrue(StreamEx.of("foo", "bar", "baz").ifEmpty(StreamEx.of(new HashSet<>())).spliterator()
                .hasCharacteristics(Spliterator.ORDERED));
        List<String> list = Collections.singletonList("foo");
        Spliterator<String> spliterator = StreamEx.<String>empty().ifEmpty(list.stream()).spliterator();
        consumeElement(spliterator, "foo");
        assertEquals(list.spliterator().characteristics(), spliterator.characteristics());
    }

    @Test
    public void testSize() {
        Spliterator<String> spliterator = StreamEx.of("foo", "bar", "baz").ifEmpty("qux").spliterator();
        assertEquals(3, spliterator.getExactSizeIfKnown());
        consumeElement(spliterator, "foo");
        assertEquals(2, spliterator.getExactSizeIfKnown());
        assertEquals(1, StreamEx.empty().ifEmpty("qux").spliterator().getExactSizeIfKnown());
    }

    @Test
    public void testFiltered() {
        List<Integer> data = IntStreamEx.range(1000).boxed().toList();
        Supplier<Spliterator<Integer>> allMatch = () -> data.parallelStream().filter(x -> x >= 0).spliterator();
        Supplier<Spliterator<Integer>> noneMatch = () -> data.parallelStream().filter(x -> x < 0).spliterator();
        Supplier<Spliterator<Integer>> lastMatch = () -> data.parallelStream().filter(x -> x == 999).spliterator();
        checkSpliterator("++", data, () -> new IfEmptySpliterator<>(allMatch.get(), allMatch.get()));
        checkSpliterator("l+", Collections.singletonList(999), () -> new IfEmptySpliterator<>(lastMatch.get(), allMatch.get()));
        checkSpliterator("-+", data, () -> new IfEmptySpliterator<>(noneMatch.get(), allMatch.get()));
        checkSpliterator("+-", data, () -> new IfEmptySpliterator<>(allMatch.get(), noneMatch.get()));
        checkSpliterator("--", Collections.emptyList(), () -> new IfEmptySpliterator<>(noneMatch.get(), noneMatch.get()));
    }
}
