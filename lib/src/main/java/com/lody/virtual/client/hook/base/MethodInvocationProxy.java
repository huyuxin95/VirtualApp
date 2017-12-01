package com.lody.virtual.client.hook.base;

import android.content.Context;

import com.lody.virtual.client.core.InvocationStubManager;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.interfaces.IInjector;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

/**
 * @author Lody
 *         <p>
 *         This class is responsible with:
 *         - Instantiating a {@link MethodInvocationStub.HookInvocationHandler} on {@link #getInvocationStub()} ()}
 *         - Install a bunch of {@link MethodProxy}s, either with a @{@link Inject} annotation or manually
 *         calling {@link #addMethodProxy(MethodProxy)} from {@link #onBindMethods()}
 *         - Install the hooked object on the Runtime via {@link #inject()}
 *         <p>
 *         All {@link MethodInvocationProxy}s (plus a couple of other @{@link IInjector}s are installed by
 *         {@link InvocationStubManager}
 *         传入他的子类,在其构造中将其构造加入需要hook的集合
 * @see Inject
 */
public abstract class MethodInvocationProxy<T extends MethodInvocationStub> implements IInjector {

    protected T mInvocationStub;

    public MethodInvocationProxy(T invocationStub) {
        this.mInvocationStub = invocationStub;
        onBindMethods();
        afterHookApply(invocationStub);

        LogInvocation loggingAnnotation = getClass().getAnnotation(LogInvocation.class);
        if (loggingAnnotation != null) {
            invocationStub.setInvocationLoggingCondition(loggingAnnotation.value());
        }
    }

    protected void onBindMethods() {
        if (mInvocationStub == null) {
            return;
        }
        //通过注解获取当前类及其内部类的类类型
        Class<? extends MethodInvocationProxy> clazz = getClass();
        Inject inject = clazz.getAnnotation(Inject.class);
        if (inject != null) {
            Class<?> proxiesClass = inject.value();
            Class<?>[] innerClasses = proxiesClass.getDeclaredClasses();
            for (Class<?> innerClass : innerClasses) {
                if (!Modifier.isAbstract(innerClass.getModifiers())
                        && MethodProxy.class.isAssignableFrom(innerClass)
                        && innerClass.getAnnotation(SkipInject.class) == null) {

                    addMethodProxy(innerClass);
                }
            }

        }
    }

    /**
     * 将构造加入需要hook的方法集合
     * @param hookType
     */
    private void addMethodProxy(Class<?> hookType) {
        //将构造加入需要hook的方法
        try {
            Constructor<?> constructor = hookType.getDeclaredConstructors()[0];
            if (!constructor.isAccessible()) {
                constructor.setAccessible(true);
            }
            MethodProxy methodProxy;
            if (constructor.getParameterTypes().length == 0) {
                methodProxy = (MethodProxy) constructor.newInstance();
            } else {
                methodProxy = (MethodProxy) constructor.newInstance(this);
            }
            mInvocationStub.addMethodProxy(methodProxy);
        } catch (Throwable e) {
            throw new RuntimeException("Unable to instance Hook : " + hookType + " : " + e.getMessage());
        }
    }

    public MethodProxy addMethodProxy(MethodProxy methodProxy) {
        return mInvocationStub.addMethodProxy(methodProxy);
    }

    protected void afterHookApply(T delegate) {
    }

    @Override
    public abstract void inject() throws Throwable;

    public Context getContext() {
        return VirtualCore.get().getContext();
    }

    public T getInvocationStub() {
        return mInvocationStub;
    }
}
