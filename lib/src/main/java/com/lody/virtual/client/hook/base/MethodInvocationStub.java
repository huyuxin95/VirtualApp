package com.lody.virtual.client.hook.base;

import android.text.TextUtils;
import android.util.Log;

import com.lody.virtual.client.hook.utils.MethodParameterUtils;
import com.lody.virtual.helper.utils.VLog;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Lody
 *         <p>
 *         HookHandler uses Java's {@link Proxy} to create a wrapper for existing services.
 *         <p>
 *         When any method is called on the wrapper, it checks if there is any {@link MethodProxy} registered
 *         and enabled for that method. If so, it calls the startUniformer instead of the wrapped implementation.
 *         <p>
 *         The whole thing is managed by a {@link MethodInvocationProxy} subclass
 *         通过动态代理在构造中创建需要hook的对象,并传入需要hook的方法的集合
 *         在HookInvocationHandler内统一处理,
 */
@SuppressWarnings("unchecked")
public class MethodInvocationStub<T> {

    private static final String TAG = MethodInvocationStub.class.getSimpleName();

    private Map<String, MethodProxy> mInternalMethodProxies = new HashMap<>();
    private T mBaseInterface;
    private T mProxyInterface;
    private String mIdentityName;
    private LogInvocation.Condition mInvocationLoggingCondition = LogInvocation.Condition.NEVER;

    /**
     * 获取所有hook的方法
     * @return
     */
    public Map<String, MethodProxy> getAllHooks() {
        return mInternalMethodProxies;
    }

    /**
     * mBaseInterface存储了服务在客户端的引用
     * mProxyInterface 存储了 服务引用的代理对象
     * @param baseInterface
     * @param proxyInterfaces
     */
    public MethodInvocationStub(T baseInterface, Class<?>... proxyInterfaces) {
        this.mBaseInterface = baseInterface;
        if (baseInterface != null) {
            if (proxyInterfaces == null) {
                //获取服务(ams)引用的所有接口
                proxyInterfaces = MethodParameterUtils.getAllInterface(baseInterface.getClass());
            }
            //生成该服务引用的代理对象
            mProxyInterface = (T) Proxy.newProxyInstance(baseInterface.getClass().getClassLoader(), proxyInterfaces, new HookInvocationHandler());
        } else {
            VLog.d(TAG, "Unable to build HookDelegate: %s.", getIdentityName());
        }
    }

    public LogInvocation.Condition getInvocationLoggingCondition() {
        return mInvocationLoggingCondition;
    }

    public void setInvocationLoggingCondition(LogInvocation.Condition invocationLoggingCondition) {
        mInvocationLoggingCondition = invocationLoggingCondition;
    }

    public void setIdentityName(String identityName) {
        this.mIdentityName = identityName;
    }

    public String getIdentityName() {
        if (mIdentityName != null) {
            return mIdentityName;
        }
        return getClass().getSimpleName();
    }


    public MethodInvocationStub(T baseInterface) {
        this(baseInterface, (Class[]) null);
    }

    /**
     * Copy all proxies from the input HookDelegate.
     *  从一个已创建的对象中copy已创建的代理方法集合
     * @param from the HookDelegate we copy from.
     */
    public void copyMethodProxies(MethodInvocationStub from) {
        this.mInternalMethodProxies.putAll(from.getAllHooks());
    }

    /**
     * Add a method proxy.
     *向map集合添加需要代理的方法
     * @param methodProxy proxy
     */
    public MethodProxy addMethodProxy(MethodProxy methodProxy) {
        if (methodProxy != null && !TextUtils.isEmpty(methodProxy.getMethodName())) {
            if (mInternalMethodProxies.containsKey(methodProxy.getMethodName())) {
                VLog.w(TAG, "The Hook(%s, %s) you added has been in existence.", methodProxy.getMethodName(),
                        methodProxy.getClass().getName());
                return methodProxy;
            }
            mInternalMethodProxies.put(methodProxy.getMethodName(), methodProxy);
        }
        return methodProxy;
    }

    /**
     * Remove a method proxy.
     * 根据方法名为索引从需要代理的方法中移除指定方法
     * @param hookName proxy
     * @return The proxy you removed
     */
    public MethodProxy removeMethodProxy(String hookName) {
        return mInternalMethodProxies.remove(hookName);
    }

    /**
     * Remove a method proxy.
     *根据方法代理对象为索引从需要代理的方法中移除指定方法
     * @param methodProxy target proxy
     */
    public void removeMethodProxy(MethodProxy methodProxy) {
        if (methodProxy != null) {
            removeMethodProxy(methodProxy.getMethodName());
        }
    }

    /**
     * Remove all method proxies.
     * 清空全部需要代理的方法
     */
    public void removeAllMethodProxies() {
        mInternalMethodProxies.clear();
    }

    /**
     * Get the startUniformer by its name.
     * 利用泛型,根据方法名获取MethodProxy及其子类对象
     * @param name name of the Hook
     * @param <H>  Type of the Hook
     * @return target startUniformer
     */
    @SuppressWarnings("unchecked")
    public <H extends MethodProxy> H getMethodProxy(String name) {
        return (H) mInternalMethodProxies.get(name);
    }

    /**
     * 获取系统服务在客户端引用的代理对象
     * 他的获取在当前类的构造的时候获取
     * @return Proxy interface
     */
    public T getProxyInterface() {
        return mProxyInterface;
    }

    /**
     * 获取通过反射获得的服务在客户端的引用
     * 它在当前类的构造的时候传入
     * @return Origin Interface
     */
    public T getBaseInterface() {
        return mBaseInterface;
    }

    /**
     * @return count of the hooks
     * 代理的方法数
     */
    public int getMethodProxiesCount() {
        return mInternalMethodProxies.size();
    }

    /**
     * 代理对象方法的hook
     */
    private class HookInvocationHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            MethodProxy methodProxy = getMethodProxy(method.getName());
            boolean useProxy = (methodProxy != null && methodProxy.isEnable());
            boolean mightLog = (mInvocationLoggingCondition != LogInvocation.Condition.NEVER) ||
                    (methodProxy != null && methodProxy.getInvocationLoggingCondition() != LogInvocation.Condition.NEVER);

            String argStr = null;
            Object res = null;
            Throwable exception = null;
            if (mightLog) {
                // Arguments to string is done before the method is called because the method might actually change it
                argStr = Arrays.toString(args);
                argStr = argStr.substring(1, argStr.length()-1);
            }


            try {
                //hook 处理逻辑通过接口回调在实现类里处理
                if (useProxy && methodProxy.beforeCall(mBaseInterface, method, args)) {
                    res = methodProxy.call(mBaseInterface, method, args);
                    res = methodProxy.afterCall(mBaseInterface, method, args, res);
                } else {
                    res = method.invoke(mBaseInterface, args);
                }
                return res;

            } catch (Throwable t) {
                exception = t;
                if (exception instanceof InvocationTargetException && ((InvocationTargetException) exception).getTargetException() != null) {
                    exception = ((InvocationTargetException) exception).getTargetException();
                }
                throw exception;

            } finally {
                if (mightLog) {
                    int logPriority = mInvocationLoggingCondition.getLogLevel(useProxy, exception != null);
                    if (methodProxy != null) {
                        logPriority = Math.max(logPriority, methodProxy.getInvocationLoggingCondition().getLogLevel(useProxy, exception != null));
                    }
                    if (logPriority >= 0) {
                        String retString;
                        if (exception != null) {
                            retString = exception.toString();
                        } else if (method.getReturnType().equals(void.class)) {
                            retString = "void";
                        } else {
                            retString = String.valueOf(res);
                        }
                        Log.println(logPriority, TAG, method.getDeclaringClass().getSimpleName() + "." + method.getName() + "(" + argStr + ") => " + retString);
                    }
                }
            }
        }
    }

    private void dumpMethodProxies() {
        StringBuilder sb = new StringBuilder(50);
        sb.append("*********************");
        for (MethodProxy proxy : mInternalMethodProxies.values()) {
            sb.append(proxy.getMethodName()).append("\n");
        }
        sb.append("*********************");
        VLog.e(TAG, sb.toString());
    }

}
