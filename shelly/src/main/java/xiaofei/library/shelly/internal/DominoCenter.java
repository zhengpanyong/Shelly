/**
 *
 * Copyright 2016 Xiaofei
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package xiaofei.library.shelly.internal;

import android.util.Log;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import xiaofei.library.shelly.Domino;

/**
 * Created by Xiaofei on 16/5/26.
 */
public class DominoCenter {

    private static final String TAG = "DominoCenter";

    private static volatile DominoCenter sInstance = null;

    private final ConcurrentHashMap<Object, Domino> mDominoes;

    private DominoCenter() {
        mDominoes = new ConcurrentHashMap<>();
    }

    public static DominoCenter getInstance() {
        if (sInstance == null) {
            synchronized (DominoCenter.class) {
                if (sInstance == null) {
                    sInstance = new DominoCenter();
                }
            }
        }
        return sInstance;
    }

    public void commit(Domino domino) {
        Object label = domino.getLabel();
        if (mDominoes.put(label, domino) != null) {
            Log.w(TAG, "Domino name duplicate! Check whether you have commit a domino with the same label before.");
        }
    }

    public void play(Object label, Object input) {
        Domino domino = mDominoes.get(label);
        if (domino == null) {
            throw new IllegalStateException("There is no domino labeled '" + label + "'.");
        }
        domino.play(input);
    }

}
