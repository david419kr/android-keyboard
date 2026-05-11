/*
 * Copyright (C) 2026 FUTO Holdings, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.futo.inputmethod.event.combiners;

import junit.framework.TestCase;

import org.futo.inputmethod.event.Event;
import org.futo.inputmethod.latin.common.Constants;

import java.util.ArrayList;

import kotlin.jvm.functions.Function0;

public class KoreanCombinerTests extends TestCase {
    private static final int TIMEOUT_MS = 200;

    private KoreanCombiner newCombiner() {
        return new KoreanCombiner(true, new Function0<Integer>() {
            @Override
            public Integer invoke() {
                return TIMEOUT_MS;
            }
        });
    }

    private static Event key(final char ch, final long eventTime) {
        return Event.createSoftwareKeypressEvent(ch, Event.NOT_A_KEY_CODE,
                Constants.NOT_A_COORDINATE, Constants.NOT_A_COORDINATE,
                false /* isKeyRepeat */, eventTime);
    }

    private static Event delete(final long eventTime) {
        return Event.createSoftwareKeypressEvent(Event.NOT_A_CODE_POINT, Constants.CODE_DELETE,
                Constants.NOT_A_COORDINATE, Constants.NOT_A_COORDINATE,
                false /* isKeyRepeat */, eventTime);
    }

    private static String compose(final KoreanCombiner combiner, final String input,
            final long... eventTimes) {
        assertEquals(input.length(), eventTimes.length);
        final ArrayList<Event> previousEvents = new ArrayList<>();
        for (int i = 0; i < input.length(); i++) {
            final Event event = key(input.charAt(i), eventTimes[i]);
            combiner.processEvent(previousEvents, event);
            previousEvents.add(event);
        }
        return combiner.getCombiningStateFeedback().toString();
    }

    public void testFastRepeatedFinalPrefersDoubleInitialBeforeVowel() {
        assertEquals("\uB098\uBE60",
                compose(newCombiner(), "\u3134\u314F\u3142\u3142\u314F",
                        0, 10, 20, 50, 60));
    }

    public void testSlowRepeatedFinalKeepsFinalConsonant() {
        assertEquals("\uB0A9\uBC14",
                compose(newCombiner(), "\u3134\u314F\u3142\u3142\u314F",
                        0, 10, 20, 250, 260));
    }

    public void testFastRepeatedGiyeokProducesDoubleInitial() {
        assertEquals("\uD558\uAF9C",
                compose(newCombiner(), "\u314E\u314F\u3131\u3131\u315B",
                        0, 10, 20, 50, 60));
    }

    public void testSlowRepeatedGiyeokKeepsBatchim() {
        assertEquals("\uD559\uAD50",
                compose(newCombiner(), "\u314E\u314F\u3131\u3131\u315B",
                        0, 10, 20, 250, 260));
    }

    public void testInitialDoubleConsonantUsesRepeatedKeyTimeout() {
        assertEquals("\uAE4C",
                compose(newCombiner(), "\u3131\u3131\u314F", 0, 50, 60));
        assertEquals("\u3131\uAC00",
                compose(newCombiner(), "\u3131\u3131\u314F", 0, 250, 260));
    }

    public void testBackspaceAlsoRemovesRepeatedKeyTimingState() {
        final KoreanCombiner combiner = newCombiner();
        final ArrayList<Event> previousEvents = new ArrayList<>();

        Event event = key('\u3131', 0);
        combiner.processEvent(previousEvents, event);
        previousEvents.add(event);

        event = key('\u3131', 300);
        combiner.processEvent(previousEvents, event);
        previousEvents.add(event);
        assertEquals("\u3131\u3131", combiner.getCombiningStateFeedback().toString());

        event = delete(310);
        combiner.processEvent(previousEvents, event);
        previousEvents.add(event);
        assertEquals("\u3131", combiner.getCombiningStateFeedback().toString());

        event = key('\u314F', 400);
        combiner.processEvent(previousEvents, event);
        previousEvents.add(event);
        assertEquals("\uAC00", combiner.getCombiningStateFeedback().toString());
    }
}
