//
// ========================================================================
// Copyright (c) 1995-2020 Mort Bay Consulting Pty Ltd and others.
//
// This program and the accompanying materials are made available under
// the terms of the Eclipse Public License 2.0 which is available at
// https://www.eclipse.org/legal/epl-2.0
//
// This Source Code may also be made available under the following
// Secondary Licenses when the conditions for such availability set
// forth in the Eclipse Public License, v. 2.0 are satisfied:
// the Apache License v2.0 which is available at
// https://www.apache.org/licenses/LICENSE-2.0
//
// SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
// ========================================================================
//

package org.eclipse.jetty.util;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TrieTest
{
    public static Stream<Arguments> implementations()
    {
        List<Trie> impls = new ArrayList<>();

        impls.add(new ArrayTrie<Integer>(128));
        impls.add(new TreeTrie<Integer>());
        impls.add(new ArrayTernaryTrie<Integer>(128));

        for (Trie<Integer> trie : impls)
        {
            trie.put("hello", 1);
            trie.put("He", 2);
            trie.put("HELL", 3);
            trie.put("wibble", 4);
            trie.put("Wobble", 5);
            trie.put("foo-bar", 6);
            trie.put("foo+bar", 7);
            trie.put("HELL4", 8);
            trie.put("", 9);
        }

        return impls.stream().map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("implementations")
    public void testOverflow(Trie<Integer> trie) throws Exception
    {
        int i = 0;
        while (true)
        {
            if (++i > 10000)
                break; // must not be fixed size
            if (!trie.put("prefix" + i, i))
            {
                assertTrue(trie.isFull());
                break;
            }
        }

        assertTrue(!trie.isFull() || !trie.put("overflow", 0));
    }

    @ParameterizedTest
    @MethodSource("implementations")
    public void testKeySet(Trie<Integer> trie) throws Exception
    {
        String[] values = new String[]{
            "hello",
            "He",
            "HELL",
            "wibble",
            "Wobble",
            "foo-bar",
            "foo+bar",
            "HELL4",
            ""
        };

        for (String value : values)
        {
            assertThat(value, is(in(trie.keySet())));
        }
    }

    @ParameterizedTest
    @MethodSource("implementations")
    public void testGetString(Trie<Integer> trie) throws Exception
    {
        assertEquals(1, trie.get("hello").intValue());
        assertEquals(2, trie.get("He").intValue());
        assertEquals(3, trie.get("HELL").intValue());
        assertEquals(4, trie.get("wibble").intValue());
        assertEquals(5, trie.get("Wobble").intValue());
        assertEquals(6, trie.get("foo-bar").intValue());
        assertEquals(7, trie.get("foo+bar").intValue());

        assertEquals(1, trie.get("Hello").intValue());
        assertEquals(2, trie.get("HE").intValue());
        assertEquals(3, trie.get("heLL").intValue());
        assertEquals(4, trie.get("Wibble").intValue());
        assertEquals(5, trie.get("wobble").intValue());
        assertEquals(6, trie.get("Foo-bar").intValue());
        assertEquals(7, trie.get("FOO+bar").intValue());
        assertEquals(8, trie.get("HELL4").intValue());
        assertEquals(9, trie.get("").intValue());

        assertEquals(null, trie.get("helloworld"));
        assertEquals(null, trie.get("Help"));
        assertEquals(null, trie.get("Blah"));
    }

    @ParameterizedTest
    @MethodSource("implementations")
    public void testGetBuffer(Trie<Integer> trie) throws Exception
    {
        assertEquals(1, trie.get(BufferUtil.toBuffer("xhellox"), 1, 5).intValue());
        assertEquals(2, trie.get(BufferUtil.toBuffer("xhellox"), 1, 2).intValue());
        assertEquals(3, trie.get(BufferUtil.toBuffer("xhellox"), 1, 4).intValue());
        assertEquals(4, trie.get(BufferUtil.toBuffer("wibble"), 0, 6).intValue());
        assertEquals(5, trie.get(BufferUtil.toBuffer("xWobble"), 1, 6).intValue());
        assertEquals(6, trie.get(BufferUtil.toBuffer("xfoo-barx"), 1, 7).intValue());
        assertEquals(7, trie.get(BufferUtil.toBuffer("xfoo+barx"), 1, 7).intValue());

        assertEquals(1, trie.get(BufferUtil.toBuffer("xhellox"), 1, 5).intValue());
        assertEquals(2, trie.get(BufferUtil.toBuffer("xHELLox"), 1, 2).intValue());
        assertEquals(3, trie.get(BufferUtil.toBuffer("xhellox"), 1, 4).intValue());
        assertEquals(4, trie.get(BufferUtil.toBuffer("Wibble"), 0, 6).intValue());
        assertEquals(5, trie.get(BufferUtil.toBuffer("xwobble"), 1, 6).intValue());
        assertEquals(6, trie.get(BufferUtil.toBuffer("xFOO-barx"), 1, 7).intValue());
        assertEquals(7, trie.get(BufferUtil.toBuffer("xFOO+barx"), 1, 7).intValue());

        assertEquals(null, trie.get(BufferUtil.toBuffer("xHelloworldx"), 1, 10));
        assertEquals(null, trie.get(BufferUtil.toBuffer("xHelpx"), 1, 4));
        assertEquals(null, trie.get(BufferUtil.toBuffer("xBlahx"), 1, 4));
    }

    @ParameterizedTest
    @MethodSource("implementations")
    public void testGetDirectBuffer(Trie<Integer> trie) throws Exception
    {
        assertEquals(1, trie.get(BufferUtil.toDirectBuffer("xhellox"), 1, 5).intValue());
        assertEquals(2, trie.get(BufferUtil.toDirectBuffer("xhellox"), 1, 2).intValue());
        assertEquals(3, trie.get(BufferUtil.toDirectBuffer("xhellox"), 1, 4).intValue());
        assertEquals(4, trie.get(BufferUtil.toDirectBuffer("wibble"), 0, 6).intValue());
        assertEquals(5, trie.get(BufferUtil.toDirectBuffer("xWobble"), 1, 6).intValue());
        assertEquals(6, trie.get(BufferUtil.toDirectBuffer("xfoo-barx"), 1, 7).intValue());
        assertEquals(7, trie.get(BufferUtil.toDirectBuffer("xfoo+barx"), 1, 7).intValue());

        assertEquals(1, trie.get(BufferUtil.toDirectBuffer("xhellox"), 1, 5).intValue());
        assertEquals(2, trie.get(BufferUtil.toDirectBuffer("xHELLox"), 1, 2).intValue());
        assertEquals(3, trie.get(BufferUtil.toDirectBuffer("xhellox"), 1, 4).intValue());
        assertEquals(4, trie.get(BufferUtil.toDirectBuffer("Wibble"), 0, 6).intValue());
        assertEquals(5, trie.get(BufferUtil.toDirectBuffer("xwobble"), 1, 6).intValue());
        assertEquals(6, trie.get(BufferUtil.toDirectBuffer("xFOO-barx"), 1, 7).intValue());
        assertEquals(7, trie.get(BufferUtil.toDirectBuffer("xFOO+barx"), 1, 7).intValue());

        assertEquals(null, trie.get(BufferUtil.toDirectBuffer("xHelloworldx"), 1, 10));
        assertEquals(null, trie.get(BufferUtil.toDirectBuffer("xHelpx"), 1, 4));
        assertEquals(null, trie.get(BufferUtil.toDirectBuffer("xBlahx"), 1, 4));
    }

    @ParameterizedTest
    @MethodSource("implementations")
    public void testGetBestArray(Trie<Integer> trie) throws Exception
    {
        assertEquals(1, trie.getBest(StringUtil.getUtf8Bytes("xhelloxxxx"), 1, 8).intValue());
        assertEquals(2, trie.getBest(StringUtil.getUtf8Bytes("xhelxoxxxx"), 1, 8).intValue());
        assertEquals(3, trie.getBest(StringUtil.getUtf8Bytes("xhellxxxxx"), 1, 8).intValue());
        assertEquals(6, trie.getBest(StringUtil.getUtf8Bytes("xfoo-barxx"), 1, 8).intValue());
        assertEquals(8, trie.getBest(StringUtil.getUtf8Bytes("xhell4xxxx"), 1, 8).intValue());

        assertEquals(1, trie.getBest(StringUtil.getUtf8Bytes("xHELLOxxxx"), 1, 8).intValue());
        assertEquals(2, trie.getBest(StringUtil.getUtf8Bytes("xHELxoxxxx"), 1, 8).intValue());
        assertEquals(3, trie.getBest(StringUtil.getUtf8Bytes("xHELLxxxxx"), 1, 8).intValue());
        assertEquals(6, trie.getBest(StringUtil.getUtf8Bytes("xfoo-BARxx"), 1, 8).intValue());
        assertEquals(8, trie.getBest(StringUtil.getUtf8Bytes("xHELL4xxxx"), 1, 8).intValue());
        assertEquals(9, trie.getBest(StringUtil.getUtf8Bytes("xZZZZZxxxx"), 1, 8).intValue());
    }

    @ParameterizedTest
    @MethodSource("implementations")
    public void testGetBestBuffer(Trie<Integer> trie) throws Exception
    {
        assertEquals(1, trie.getBest(BufferUtil.toBuffer("xhelloxxxx"), 1, 8).intValue());
        assertEquals(2, trie.getBest(BufferUtil.toBuffer("xhelxoxxxx"), 1, 8).intValue());
        assertEquals(3, trie.getBest(BufferUtil.toBuffer("xhellxxxxx"), 1, 8).intValue());
        assertEquals(6, trie.getBest(BufferUtil.toBuffer("xfoo-barxx"), 1, 8).intValue());
        assertEquals(8, trie.getBest(BufferUtil.toBuffer("xhell4xxxx"), 1, 8).intValue());

        assertEquals(1, trie.getBest(BufferUtil.toBuffer("xHELLOxxxx"), 1, 8).intValue());
        assertEquals(2, trie.getBest(BufferUtil.toBuffer("xHELxoxxxx"), 1, 8).intValue());
        assertEquals(3, trie.getBest(BufferUtil.toBuffer("xHELLxxxxx"), 1, 8).intValue());
        assertEquals(6, trie.getBest(BufferUtil.toBuffer("xfoo-BARxx"), 1, 8).intValue());
        assertEquals(8, trie.getBest(BufferUtil.toBuffer("xHELL4xxxx"), 1, 8).intValue());
        assertEquals(9, trie.getBest(BufferUtil.toBuffer("xZZZZZxxxx"), 1, 8).intValue());

        ByteBuffer buffer = (ByteBuffer)BufferUtil.toBuffer("xhelloxxxxxxx").position(2);
        assertEquals(1, trie.getBest(buffer, -1, 10).intValue());
    }

    @ParameterizedTest
    @MethodSource("implementations")
    public void testGetBestDirectBuffer(Trie<Integer> trie) throws Exception
    {
        assertEquals(1, trie.getBest(BufferUtil.toDirectBuffer("xhelloxxxx"), 1, 8).intValue());
        assertEquals(2, trie.getBest(BufferUtil.toDirectBuffer("xhelxoxxxx"), 1, 8).intValue());
        assertEquals(3, trie.getBest(BufferUtil.toDirectBuffer("xhellxxxxx"), 1, 8).intValue());
        assertEquals(6, trie.getBest(BufferUtil.toDirectBuffer("xfoo-barxx"), 1, 8).intValue());
        assertEquals(8, trie.getBest(BufferUtil.toDirectBuffer("xhell4xxxx"), 1, 8).intValue());

        assertEquals(1, trie.getBest(BufferUtil.toDirectBuffer("xHELLOxxxx"), 1, 8).intValue());
        assertEquals(2, trie.getBest(BufferUtil.toDirectBuffer("xHELxoxxxx"), 1, 8).intValue());
        assertEquals(3, trie.getBest(BufferUtil.toDirectBuffer("xHELLxxxxx"), 1, 8).intValue());
        assertEquals(6, trie.getBest(BufferUtil.toDirectBuffer("xfoo-BARxx"), 1, 8).intValue());
        assertEquals(8, trie.getBest(BufferUtil.toDirectBuffer("xHELL4xxxx"), 1, 8).intValue());
        assertEquals(9, trie.getBest(BufferUtil.toDirectBuffer("xZZZZZxxxx"), 1, 8).intValue());

        ByteBuffer buffer = (ByteBuffer)BufferUtil.toDirectBuffer("xhelloxxxxxxx").position(2);
        assertEquals(1, trie.getBest(buffer, -1, 10).intValue());
    }

    @ParameterizedTest
    @MethodSource("implementations")
    public void testFull(Trie<Integer> trie) throws Exception
    {
        if (!(trie instanceof ArrayTrie<?> || trie instanceof ArrayTernaryTrie<?>))
            return;

        assertFalse(trie.put("Large: This is a really large key and should blow the maximum size of the array trie as lots of nodes should already be used.", 99));
        testGetString(trie);
        testGetBestArray(trie);
        testGetBestBuffer(trie);
    }

    public static int requiredCapacity(Set<String> keys)
    {
        List<String> list = new ArrayList<>(keys);
        Collections.sort(list);
        return requiredCapacity(list, 0, list.size(), 0);
    }

    private static int requiredCapacity(List<String> keys, int offset, int length, int index)
    {
        if (length == 0)
            return 0;

        int required = 0;

        Character c = null;
        for (int i = 0; i < length; i++)
        {
            String k = keys.get(offset + i);
            if (k.length() <= index)
                continue;
            char n = k.charAt(index);
            if (c == null)
            {
                required++;
                c = n;
                offset += i;
                length -= i;
            }
            else if (c != n)
            {
                required +=  requiredCapacity(keys, offset, i, index + 1) + 1;
                offset += i;
                length -= i;
                c = n;
                i = 1;
            }
        }

        if (c != null)
        {
            required += requiredCapacity(keys, offset, length, index + 1);
        }
        return required;
    }

    @Test
    public void testRequiredCapacity()
    {
        assertThat(requiredCapacity(Set.of("A", "ABCD")), is(4));

        assertThat(requiredCapacity(Set.of("ABC")), is(3));
        assertThat(requiredCapacity(Set.of("ABC", "XYZ")), is(6));
        assertThat(requiredCapacity(Set.of("A00", "A11")), is(5));
        assertThat(requiredCapacity(Set.of("A00", "A01", "A10", "A11")), is(7));
        assertThat(requiredCapacity(Set.of("A", "AB")), is(2));
        assertThat(requiredCapacity(Set.of("A", "ABC")), is(3));
        assertThat(requiredCapacity(Set.of("A", "ABCD")), is(4));
        assertThat(requiredCapacity(Set.of("AB", "ABC")), is(3));
        assertThat(requiredCapacity(Set.of("ABC", "ABCD")), is(4));
        assertThat(requiredCapacity(Set.of("ABC", "ABCDEF")), is(6));
        assertThat(requiredCapacity(Set.of("AB", "A")), is(2));
        assertThat(requiredCapacity(Set.of("ABC", "ABCDEF")), is(6));
        assertThat(requiredCapacity(Set.of("ABCDEF", "ABC")), is(6));
        assertThat(requiredCapacity(Set.of("ABC", "ABCDEF", "ABX")), is(7));
        assertThat(requiredCapacity(Set.of("ABCDEF", "ABC", "ABX")), is(7));
        assertThat(requiredCapacity(Set.of("ADEF", "AQPR4", "AQZ")), is(9));
        assertThat(requiredCapacity(Set.of("111", "ADEF", "AQPR4", "AQZ", "999")), is(15));
    }
}
