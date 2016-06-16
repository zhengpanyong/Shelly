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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import xiaofei.library.shelly.annotation.AnnotationUtils;

/**
 * Created by Xiaofei on 16/5/26.
 */
public class TargetCenter {

    private static volatile TargetCenter sInstance = null;

    private final ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, Method>> mMethods;

    private final ConcurrentHashMap<Class<?>, CopyOnWriteArrayList<Object>> mObjects;

    private TargetCenter() {
        mMethods = new ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, Method>>();
        mObjects = new ConcurrentHashMap<Class<?>, CopyOnWriteArrayList<Object>>();
    }

    public static TargetCenter getInstance() {
        if (sInstance == null) {
            synchronized (TargetCenter.class) {
                if (sInstance == null) {
                    sInstance = new TargetCenter();
                }
            }
        }
        return sInstance;
    }

    public void register(Object object) {
        Class<?> clazz = object.getClass();
        CopyOnWriteArrayList<Object> objects = mObjects.get(clazz);
        if (objects == null) {
            objects = mObjects.putIfAbsent(clazz, new CopyOnWriteArrayList<>());
        }
        objects.add(object);
        //The following must be in a synchronized block.
        //The mMethods modification must follow the mObjects modification.
        synchronized (mMethods) {
            ConcurrentHashMap<String, Method> methods = mMethods.get(clazz);
            if (methods == null) {
                mMethods.putIfAbsent(clazz, new ConcurrentHashMap<String, Method>(AnnotationUtils.getTargetMethods(clazz)));
            }
        }
    }

    public void unregister(Object object) {
        Class<?> clazz = object.getClass();
        CopyOnWriteArrayList<Object> objects = mObjects.get(clazz);
        if (objects == null || !objects.remove(object)) {
            throw new IllegalArgumentException("Object " + object + " has not been registered.");
        }
        //The following must be in a synchronized block.
        synchronized (mMethods) {
            if (objects.isEmpty()) {
                mMethods.remove(clazz);
            }
        }
    }

    public List<Object> getObjects(Class<?> clazz) {
        synchronized (mObjects) {
            return Collections.unmodifiableList(mObjects.get(clazz));
        }
    }

    public void call(Class<?> clazz, String target, Object input) {
        ConcurrentHashMap<String, Method> methods = mMethods.get(clazz);
        if (methods == null) {
            throw new IllegalStateException("Class " + clazz.getName() + " has not been registered.");
        }
        Method method = methods.get(target);
        if (method == null) {
            throw new IllegalStateException("Class " + clazz.getName() + " has no method matching the target " + target);
        }
        CopyOnWriteArrayList<Object> objects = mObjects.get(clazz);
        if (objects == null) {
            return;
        }
        for (Object object : objects) {
            try {
                if (method.getParameterTypes().length == 0) {
                    method.invoke(object);
                } else {
                    method.invoke(object, input);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
