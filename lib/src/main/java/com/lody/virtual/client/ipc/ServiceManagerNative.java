package com.lody.virtual.client.ipc;

import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.helper.compat.BundleCompat;
import com.lody.virtual.helper.utils.VLog;
import com.lody.virtual.server.ServiceCache;
import com.lody.virtual.server.interfaces.IServiceFetcher;

/**
 * @author Lody
 * 服务的实现在对应的AIDL的服务端实现类中
 * 这个类提供了binder连接池的入口,以及封装好连接池的获取,增加,删除操作
 * 需要注意的是获得是的服务的ibinder对象,需要通过XXXX.stub.asinterface
 * 来转换成对应服务在本地的的引用
 */
public class ServiceManagerNative {

    public static final String PACKAGE = "package";
    public static final String ACTIVITY = "activity";
    public static final String USER = "user";
    public static final String APP = "app";
    public static final String ACCOUNT = "account";
    public static final String JOB = "job";
    public static final String NOTIFICATION = "notification";
    public static final String VS = "vs";
    public static final String DEVICE = "device";
    public static final String VIRTUAL_LOC = "virtual-loc";

    public static final String SERVICE_DEF_AUTH = "virtual.service.BinderProvider";
    private static final String TAG = ServiceManagerNative.class.getSimpleName();
    public static String SERVICE_CP_AUTH = "virtual.service.BinderProvider";

    private static IServiceFetcher sFetcher;

    /**
     * 获取Binder连接池入口在本地的引用
     * @return
     */
    private static IServiceFetcher getServiceFetcher() {
        if (sFetcher == null || !sFetcher.asBinder().isBinderAlive()) {
            synchronized (ServiceManagerNative.class) {
                Context context = VirtualCore.get().getContext();
                //调用ContentProvider的call方法
                Bundle response = new ProviderCall.Builder(context, SERVICE_CP_AUTH).methodName("@").call();
                if (response != null) {
                    //获取Binder连接池入口在本地的句柄
                    IBinder binder = BundleCompat.getBinder(response, "_VA_|_binder_");
                    //设置死亡代理
                    linkBinderDied(binder);
                    sFetcher = IServiceFetcher.Stub.asInterface(binder);
                }
            }
        }
        return sFetcher;
    }

    /**
     * 通过打开ContentProvider的方式创建一个新的进程,这个新的进程的ContentProvider模拟了
     * Android的ServiceManger的角色
     */
    public static void ensureServerStarted() {
        new ProviderCall.Builder(VirtualCore.get().getContext(), SERVICE_CP_AUTH).methodName("ensure_created").call();
    }

    public static void clearServerFetcher() {
        sFetcher = null;
    }

    private static void linkBinderDied(final IBinder binder) {
        IBinder.DeathRecipient deathRecipient = new IBinder.DeathRecipient() {
            @Override
            public void binderDied() {
                binder.unlinkToDeath(this, 0);
            }
        };
        try {
            binder.linkToDeath(deathRecipient, 0);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     *     名字作为索引,检索binder连接池中已hook的服务的ibinder对象
     */
    public static IBinder getService(String name) {
        if (VirtualCore.get().isServerProcess()) {
            return ServiceCache.getService(name);
        }
        IServiceFetcher fetcher = getServiceFetcher();
        if (fetcher != null) {
            try {
                return fetcher.getService(name);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        VLog.e(TAG, "GetService(%s) return null.", name);
        return null;
    }

    /**
     * 将服务的索引和ibinder对象通过连接池引用,添加到服务端的map中
     * @param name
     * @param service
     */
    public static void addService(String name, IBinder service) {
        IServiceFetcher fetcher = getServiceFetcher();
        if (fetcher != null) {
            try {
                fetcher.addService(name, service);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 通过连接池引用,将服务从服务端的map中移除
     * @param name
     */
    public static void removeService(String name) {
        IServiceFetcher fetcher = getServiceFetcher();
        if (fetcher != null) {
            try {
                fetcher.removeService(name);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

}
