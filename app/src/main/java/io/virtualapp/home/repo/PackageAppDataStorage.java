package io.virtualapp.home.repo;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.remote.InstalledAppInfo;

import java.util.HashMap;
import java.util.Map;

import io.virtualapp.VApp;
import io.virtualapp.abs.Callback;
import io.virtualapp.abs.ui.VUiKit;
import io.virtualapp.home.models.PackageAppData;

/**
 * @author Lody
 *         <p>
 *         Cache the loaded PackageAppData.
 */
public class PackageAppDataStorage {

    private static final PackageAppDataStorage STORAGE = new PackageAppDataStorage();
    private final Map<String, PackageAppData> packageDataMap = new HashMap<>();

    public static PackageAppDataStorage get() {
        return STORAGE;
    }

    /**
     * 根据packagename获取到安装包的信息
     * @param packageName
     * @return
     */
    public PackageAppData acquire(String packageName) {
        PackageAppData data;
        synchronized (packageDataMap) {
            data = packageDataMap.get(packageName);
            if (data == null) {
                data = loadAppData(packageName);
            }
        }
        return data;
    }

    public void acquire(String packageName, Callback<PackageAppData> callback) {
        VUiKit.defer()
                .when(() -> acquire(packageName))
                .done(callback::callback);
    }

    /**
     * 获取安装包新的,并用Map<String, PackageAppData>  维护起来
     * @param packageName
     * @return
     */
    private PackageAppData loadAppData(String packageName) {
        InstalledAppInfo setting = VirtualCore.get().getInstalledAppInfo(packageName, 0);
        if (setting != null) {
            PackageAppData data = new PackageAppData(VApp.getApp(), setting);
            synchronized (packageDataMap) {
                packageDataMap.put(packageName, data);
            }
            return data;
        }
        return null;
    }

}
